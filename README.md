# alfresco-es-mlt-search
Alfresco Elasticsearch More Like This Search

## es-search-mlt-api

Added endpoint

```
curl --location --request POST 'http://localhost:8080/alfresco/api/-default-/public/search/versions/1/mltsearch' \
--header 'Authorization: Basic YWRtaW46YWRtaW4=' \
--header 'Content-Type: application/json' \
--data-raw '{ "nodeId": "d1d1af5b-8cfa-421d-a339-f1d2f94b0499" }'
```
