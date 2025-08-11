package com.ledger;

import org.graalvm.polyglot.HostAccess;

/**
 * Marker interface for instruments that can be implemented in the guest VM
 * and invoked from the host via GraalVM. Extends the byte[] Instrument API
 * used by the host service.
 */
@HostAccess.Implementable
public interface GraalVMInstrument extends Instrument {
}
