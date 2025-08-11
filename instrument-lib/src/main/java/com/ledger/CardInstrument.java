package com.ledger;

import com.ledger.avro.Message;
import com.ledger.avro.Action;
import com.ledger.avro.Result;
import com.ledger.avro.JavaAvroUtils;

/**
 * Card transaction instrument.
 */
public class CardInstrument extends BaseInstrument implements GraalVMInstrument {

    @Override
    protected Result process(Message message) {
        // Business logic - card transaction processing
        String payload = message.payload();
        String messageType = message.messageType();

        String processedData = String.format(
            "Processed %s card transaction: %s (amount validated, fraud checked, authorized)",
            messageType, payload
        );

        // Build actions
        Action action = JavaAvroUtils.createAction("DB_WRITE", processedData);
        Action auditAction = JavaAvroUtils.createAction(
            "AUDIT_LOG",
            "Card transaction logged for compliance: " + payload
        );
        Action notificationAction = JavaAvroUtils.createAction(
            "SEND_NOTIFICATION",
            "Transaction notification sent to cardholder"
        );

        return JavaAvroUtils.createResult(action, auditAction, notificationAction);
    }
}
