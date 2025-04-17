package com.fabric.service.impl;

import com.fabric.database.dto.order.OrderItemEmail;
import com.fabric.database.entity.Order;
import com.fabric.service.EmailService;
import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetRequest;
import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.resource.Emailv31;
import jakarta.transaction.Transactional;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class EmailServiceImpl implements EmailService {
    private final MailjetClient mailjetClient;

    @Value("${mailjet.api.sender}")
    private String senderEmail;

    private static final long GMAIL_TEMPLATE_ID = 6813354;
    private static final long ABV_TEMPLATE_ID = 6813349;
    private static final long RESET_PASSWORD_TEMPLATE_ID = 6813357;

    public EmailServiceImpl(MailjetClient mailjetClient) {
        this.mailjetClient = mailjetClient;
    }

    @Override
    @Transactional
    public void sendOrderEmail(Order order) {
        try {
            JSONObject fromObject = createFromObject();
            String customerName = order.getFirstName() + " " + order.getLastName();
            JSONArray toArray = createToArray(order.getEmail(), customerName);
            JSONArray itemsArray = createItemsArray(order);
            JSONObject variablesObject = createVariablesObject(order, itemsArray, customerName);
            JSONObject messageObject = createMessageObject(fromObject, toArray, getTemplateId(order.getEmail()), "Успешна поръчка", variablesObject);
            JSONArray message = new JSONArray().put(messageObject);

            MailjetRequest request = new MailjetRequest(Emailv31.resource)
                    .property(Emailv31.MESSAGES, message);

            this.mailjetClient.post(request);
        } catch (MailjetException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendPasswordResetEmail(String email, String resetLink) {
        try {
            JSONObject fromObject = createFromObject();
            JSONArray toArray = createToArray(email, "");
            JSONObject variablesObject = new JSONObject()
                    .put("resetLink", resetLink);
            JSONObject messageObject = createMessageObject(fromObject, toArray, RESET_PASSWORD_TEMPLATE_ID, "Възстановяване на парола", variablesObject);
            JSONArray message = new JSONArray().put(messageObject);
            MailjetRequest request = new MailjetRequest(Emailv31.resource)
                    .property(Emailv31.MESSAGES, message);

            this.mailjetClient.post(request);
        } catch (MailjetException e) {
            e.printStackTrace();
        }
    }

    private JSONObject createFromObject() {
        return new JSONObject()
                .put("Email", this.senderEmail)
                .put("Name", "Fabric");
    }

    private JSONArray createToArray(String email, String name) {
        JSONObject toObject = new JSONObject()
                .put("Email", email)
                .put("Name", name);
        return new JSONArray().put(toObject);
    }

    private JSONArray createItemsArray(Order order) {
        JSONArray itemsArray = new JSONArray();
        order.getItems().stream()
                .map(item -> {
                    String path = item.getClothing().getImages().getFirst().getPublicId().contains("_F")
                            ? item.getClothing().getImages().getFirst().getPublicId()
                            : item.getClothing().getImages().get(1).getPublicId();
                    return new OrderItemEmail(
                            item.getClothing().getName(),
                            "/" + path,
                            item.getClothing().getModel(),
                            item.getSize(),
                            String.valueOf(item.getQuantity()),
                            String.format("%.2f", item.getPrice())
                    );
                })
                .forEach(orderItem -> {
                    JSONObject itemJson = new JSONObject();
                    itemJson.put("name", orderItem.getName());
                    itemJson.put("path", orderItem.getPath());
                    itemJson.put("model", orderItem.getModel());
                    itemJson.put("size", extractSize(orderItem.getSize()));
                    itemJson.put("quantity", orderItem.getQuantity() != null ? orderItem.getQuantity() : "1");
                    itemJson.put("price", orderItem.getPrice());
                    itemsArray.put(itemJson);
                });
        return itemsArray;
    }

    private JSONObject createVariablesObject(Order order, JSONArray itemsArray, String customerName) {
        String[] date = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .withZone(ZoneId.of("Europe/Sofia"))
                .format(order.getCreatedAt()).split(" ");
        String deliveryCost = order.getDeliveryCost() == 0 ? "Безплатна" : String.format("%.2f", order.getDeliveryCost()) + " лв";

        return new JSONObject()
                .put("subtotal", String.format("%.2f", order.getTotalPrice()))
                .put("shipping", deliveryCost)
                .put("total", String.format("%.2f", order.getFinalPrice()))
                .put("orderDate", date[0])
                .put("orderDateHour", date[1])
                .put("orderNumber", order.getId() + 600)
                .put("customerAddress", order.getAddress())
                .put("customerPhone", order.getPhoneNumber())
                .put("customerName", customerName)
                .put("items", itemsArray);
    }

    private JSONObject createMessageObject(JSONObject fromObject, JSONArray toArray, Long templateId, String subject, JSONObject variablesObject) {
        return new JSONObject()
                .put(Emailv31.Message.FROM, fromObject)
                .put(Emailv31.Message.TO, toArray)
                .put(Emailv31.Message.TEMPLATEID, templateId)
                .put(Emailv31.Message.TEMPLATELANGUAGE, true)
                .put(Emailv31.Message.SUBJECT, subject)
                .put(Emailv31.Message.VARIABLES, variablesObject);
    }

    private long getTemplateId(String email) {
        return email.contains("abv.bg") ? ABV_TEMPLATE_ID : GMAIL_TEMPLATE_ID;
    }

    private static String extractSize(String input) {
        int index = input.indexOf(' ');

        if (index == -1) {
            index = input.indexOf('(');
        }

        return (index == -1) ? input : input.substring(0, index);
    }
}
