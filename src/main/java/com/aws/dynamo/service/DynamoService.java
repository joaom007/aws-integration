package com.aws.dynamo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DynamoService {

    private final DynamoDbClient dynamoDbClient;

    @Value("${aws.dynamodb.table}")
    private String tableName;

    public DynamoService(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    public String save(String id, String nome) {

        Map<String, AttributeValue> item = new HashMap<>();

        item.put("id", AttributeValue.builder().s(id).build());
        item.put("nome", AttributeValue.builder().s(nome).build());

        PutItemRequest request = PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build();

        dynamoDbClient.putItem(request);

        return "Item salvo com sucesso";
    }

    public Map<String, String> find(String id) {

        Map<String, AttributeValue> key = new HashMap<>();
        key.put("id", AttributeValue.builder().s(id).build());

        GetItemRequest request = GetItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .build();

        Map<String, AttributeValue> item = dynamoDbClient.getItem(request).item();

        if (item == null || item.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, String> result = new HashMap<>();
        item.forEach((k, v) -> result.put(k, v.s()));

        return result;
    }

    public String update(String id, String nome) {

        Map<String, AttributeValue> key = new HashMap<>();
        key.put("id", AttributeValue.builder().s(id).build());

        Map<String, AttributeValueUpdate> updates = new HashMap<>();
        updates.put("nome", AttributeValueUpdate.builder()
                .value(AttributeValue.builder().s(nome).build())
                .action(AttributeAction.PUT)
                .build());

        UpdateItemRequest request = UpdateItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .attributeUpdates(updates)
                .build();

        dynamoDbClient.updateItem(request);

        return "Item atualizado com sucesso";
    }

    public String delete(String id) {

        Map<String, AttributeValue> key = new HashMap<>();
        key.put("id", AttributeValue.builder().s(id).build());

        DeleteItemRequest request = DeleteItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .build();

        dynamoDbClient.deleteItem(request);

        return "Item removido com sucesso";
    }

    public List<Map<String, String>> query(String id) {

        Map<String, AttributeValue> values = new HashMap<>();
        values.put(":id", AttributeValue.builder().s(id).build());

        QueryRequest request = QueryRequest.builder()
                .tableName(tableName)
                .keyConditionExpression("id = :id")
                .expressionAttributeValues(values)
                .build();

        List<Map<String, AttributeValue>> items = dynamoDbClient.query(request).items();

        return toStringList(items);
    }

    public List<Map<String, String>> scan() {

        ScanRequest request = ScanRequest.builder()
                .tableName(tableName)
                .build();

        List<Map<String, AttributeValue>> items = dynamoDbClient.scan(request).items();

        return toStringList(items);
    }

    public List<Map<String, String>> batchGet(List<String> ids) {

        List<Map<String, AttributeValue>> keys = new ArrayList<>();
        for (String id : ids) {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("id", AttributeValue.builder().s(id).build());
            keys.add(key);
        }

        KeysAndAttributes keysAndAttributes = KeysAndAttributes.builder()
                .keys(keys)
                .build();

        BatchGetItemRequest request = BatchGetItemRequest.builder()
                .requestItems(Map.of(tableName, keysAndAttributes))
                .build();

        List<Map<String, AttributeValue>> items =
                dynamoDbClient.batchGetItem(request).responses().get(tableName);

        return toStringList(items);
    }

    public String batchWrite(List<Map<String, String>> itens) {

        List<WriteRequest> writeRequests = new ArrayList<>();

        for (Map<String, String> dados : itens) {
            Map<String, AttributeValue> item = new HashMap<>();
            dados.forEach((k, v) -> item.put(k, AttributeValue.builder().s(v).build()));

            writeRequests.add(WriteRequest.builder()
                    .putRequest(PutRequest.builder().item(item).build())
                    .build());
        }

        BatchWriteItemRequest request = BatchWriteItemRequest.builder()
                .requestItems(Map.of(tableName, writeRequests))
                .build();

        dynamoDbClient.batchWriteItem(request);

        return "Itens salvos em lote com sucesso";
    }

    private List<Map<String, String>> toStringList(List<Map<String, AttributeValue>> items) {

        List<Map<String, String>> result = new ArrayList<>();

        if (items == null) {
            return result;
        }

        for (Map<String, AttributeValue> item : items) {
            Map<String, String> converted = new HashMap<>();
            item.forEach((k, v) -> converted.put(k, v.s()));
            result.add(converted);
        }

        return result;
    }
}