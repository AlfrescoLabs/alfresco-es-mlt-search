package org.alfresco.rest.api.ext.search;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.alfresco.repo.search.impl.elasticsearch.client.ElasticsearchHttpClientFactory;
import org.alfresco.repo.search.impl.elasticsearch.permission.ElasticsearchPermissionQueryFactory;
import org.alfresco.rest.framework.resource.EntityResource;
import org.alfresco.rest.framework.resource.actions.interfaces.EntityResourceAction;
import org.alfresco.rest.framework.resource.parameters.Parameters;
import org.alfresco.service.cmr.repository.NodeRef;
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
import org.springframework.beans.factory.annotation.Autowired;

@EntityResource(name="mltsearch", title = "MoreLikeThis")
public class MltSearchApiWebscript implements EntityResourceAction.Create<NodeRef>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(MltSearchApiWebscript.class);
    /** Hardcoded index name. */
    public static final String INDEX_NAME = "alfresco";
    @Autowired
    private ElasticsearchPermissionQueryFactory elasticsearchPermissionQueryFactory;
    /** Hardcoded to true. */
    private boolean includeGroupsForRoleAdmin = true;
    @Autowired
    private ElasticsearchHttpClientFactory httpClientFactory;

    @Override
    public List<NodeRef> create(List<NodeRef> targetNodes, Parameters parameters)
    {
        try
        {
            // Build MLT query.
            String[] fields = new String[]{"cm%3Acontent"};
            Item[] likeItems = targetNodes.stream()
                                          .map(nodeRef -> new Item(INDEX_NAME, nodeRef.getId()))
                                          .collect(Collectors.toList())
                                          .toArray(new Item[targetNodes.size()]);
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

            List<NodeRef> results = new ArrayList<>();
            for (SearchHit hit : searchResponse.getHits().getHits())
            {
                results.add(new NodeRef(hit.field("_id").getValue()));
            }
            return results;
        }
        catch (UnsupportedOperationException exception)
        {
            throw exception;
        }
        catch (Exception exception)
        {
            LOGGER.debug("Exception while executing query.", exception);
            throw new RuntimeException(exception);
        }
    }

}
