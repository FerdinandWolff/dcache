package org.dcache.webadmin.model.dataaccess.communication;

import diskCacheV111.vehicles.Message;
import dmg.cells.nucleus.CellPath;
import org.dcache.webadmin.model.dataaccess.communication.CellMessageGenerator.CellMessageRequest;

/**
 * Generates messages to be send and acts as a container for the requests generated.
 * So that they can easily retrieved via an iterator.
 * @author jans
 */
public interface CellMessageGenerator<M extends Message> extends Iterable<CellMessageRequest<M>> {

    public int getNumberOfMessages();

    interface CellMessageRequest<M extends Message> {

        public Message getPayload();

        public Class<M> getPayloadType();

        public boolean isSuccessful();

        public void setSuccessful(boolean successful);

        public CellPath getDestination();
    }
}
