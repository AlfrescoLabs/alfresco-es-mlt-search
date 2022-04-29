package org.alfresco.rest.api.ext.search;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.alfresco.repo.search.impl.elasticsearch.client.ElasticsearchHttpClientFactory;
import org.alfresco.repo.search.impl.elasticsearch.permission.ElasticsearchPermissionQueryFactory;
import org.alfresco.rest.framework.resource.EntityResource;
import org.alfresco.rest.framework.resource.actions.interfaces.EntityResourceAction;
import org.alfresco.rest.framework.resource.parameters.Parameters;
import org.alfresco.rest.api.model.Node;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.index.query.MoreLikeThisQueryBuilder.Item;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EntityResource(name="mltsearch", title = "MoreLikeThis")
public class MltSearchApiWebscript implements EntityResourceAction.Create<Node>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(MltSearchApiWebscript.class);
    /** Hardcoded index name. */
    public static final String INDEX_NAME = "alfresco";
    private ElasticsearchPermissionQueryFactory elasticsearchPermissionQueryFactory;
    /** Hardcoded to true. */
    private boolean includeGroupsForRoleAdmin = true;
    private ElasticsearchHttpClientFactory httpClientFactory;

    @Override
    public List<Node> create(List<Node> list, Parameters parameters)
    {
        try
        {
            // Build MLT query.
            String[] fields = new String[]{"cm%3Acontent"};
            Item[] likeItems = list.stream()
                                          .map(nodeRef -> new Item(INDEX_NAME, nodeRef.getNodeId()))
                                          .collect(Collectors.toList())
                                          .toArray(new Item[list.size()]);
            QueryBuilder query = QueryBuilders.moreLikeThisQuery(fields, null, likeItems);

            // Add permission checking.
            QueryBuilder queryWithPermissions = elasticsearchPermissionQueryFactory
                    .getQueryWithPermissionFilter(query,
                            this.includeGroupsForRoleAdmin);

            SearchSourceBuilder requestBuilder = new SearchSourceBuilder()
                    .query(queryWithPermissions).trackScores(true);

            LOGGER.debug("Execute query request: {}", requestBuilder);

            String indexName = INDEX_NAME;
            SearchRequest searchRequest = new SearchRequest(indexName);
            SearchResponse searchResponse = httpClientFactory.getElasticsearchClient()
                                                             .search(searchRequest.source(requestBuilder),
                                                                     RequestOptions.DEFAULT);

            LOGGER.debug("Response from query {}", searchResponse.toString());

            List<Node> results = new ArrayList<>();
            for (SearchHit hit : searchResponse.getHits().getHits())
            {
                Node node = new Node();
                node.setNodeId(hit.field("_id").getValue());
                results.add(node);
            }
            return results;
        }
        catch (UnsupportedOperationException exception)
        {
            throw exception;
        }
        catch (Exception exception)
        {
            LOGGER.error("Exception while executing query.", exception);
            throw new RuntimeException(exception);
        }
    }

    public void setElasticsearchPermissionQueryFactory(ElasticsearchPermissionQueryFactory elasticsearchPermissionQueryFactory)
    {
        this.elasticsearchPermissionQueryFactory = elasticsearchPermissionQueryFactory;
    }

    public void setHttpClientFactory(ElasticsearchHttpClientFactory httpClientFactory)
    {
        this.httpClientFactory = httpClientFactory;
    }
}
