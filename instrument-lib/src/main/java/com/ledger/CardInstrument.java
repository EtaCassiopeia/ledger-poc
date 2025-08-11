package com.ledger;

import com.ledger.avro.Message;
import com.ledger.avro.Action;
import com.ledger.avro.Result;
import com.ledger.avro.JavaAvroUtils;

/**
 * Card transaction instrument implementation.
 */
public class CardInstrument implements GraalVMInstrument {

    @Override
    public byte[] process(byte[] avroInput) {
        try {
            // Deserialize input Avro to Message
            Message message = JavaAvroUtils.deserializeMessage(avroInput);

            // Pure business logic - card transaction processing
            String payload = message.payload();
            String messageType = message.messageType();
            
            // Simulate card transaction validation and processing
            String processedData = String.format(
                "Processed %s card transaction: %s (amount validated, fraud checked, authorized)", 
                messageType, payload
            );

            // Create actions using Avro model
            Action action = JavaAvroUtils.createAction("DB_WRITE", processedData);
            
            // Add additional actions based on business logic
            Action auditAction = JavaAvroUtils.createAction("AUDIT_LOG", 
                "Card transaction logged for compliance: " + payload);
            
            Action notificationAction = JavaAvroUtils.createAction("SEND_NOTIFICATION", 
                "Transaction notification sent to cardholder");

            // Create result with multiple actions
            Result result = JavaAvroUtils.createResult(action, auditAction, notificationAction);

            // Serialize to Avro binary format
            return JavaAvroUtils.serializeResult(result);
            
        } catch (Exception e) {
            // Create error response in the expected format
            try {
                Action errorAction = JavaAvroUtils.createAction("ERROR", 
                    "Card instrument failed: " + e.getMessage());
                Result errorResult = JavaAvroUtils.createResult(errorAction);
                return JavaAvroUtils.serializeResult(errorResult);
            } catch (Exception avroError) {
                throw new RuntimeException("Critical error in card instrument", e);
            }
        }
    }
}
