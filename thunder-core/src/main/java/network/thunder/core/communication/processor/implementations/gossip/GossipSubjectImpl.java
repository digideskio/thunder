package network.thunder.core.communication.processor.implementations.gossip;

import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.P2PDataObject;
import network.thunder.core.database.DBHandler;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * Created by matsjerratsch on 01/12/2015.
 */
public class GossipSubjectImpl implements GossipSubject {

    DBHandler dbHandler;

    List<NodeObserver> observerList = new ArrayList<>();

    Map<NodeObserver, List<P2PDataObject>> dataObjectMap = new HashMap<>();
    Set<ByteBuffer> objectsKnownAlready = new HashSet<>();

    int objectsInsertedSinceLastBroadcast = 0;

    public GossipSubjectImpl (DBHandler dbHandler) {
        this.dbHandler = dbHandler;
    }

    @Override
    public void registerObserver (NodeObserver observer) {
        observerList.add(observer);
        dataObjectMap.put(observer, new ArrayList<>());
    }

    @Override
    public void removeObserver (NodeObserver observer) {
        observerList.remove(observer);
        dataObjectMap.remove(observer);
    }

    @Override
    public void newDataObjects (NodeObserver nodeObserver, List<P2PDataObject> dataObjects) {
        List<P2PDataObject> objectsToInsertIntoDatabase = new ArrayList<>();
        for (P2PDataObject dataObject : dataObjects) {
            boolean newEntry = insertNewDataObject(nodeObserver, dataObject);
            if (newEntry) {
                objectsToInsertIntoDatabase.add(dataObject);
            }
        }
        dbHandler.syncDatalist(objectsToInsertIntoDatabase);
        broadcast();
    }

    @Override
    public List<P2PDataObject> getUpdates () {
        return null;
    }

    @Override
    public boolean knowsObjectAlready (byte[] hash) {
        return objectsKnownAlready.contains(ByteBuffer.wrap(hash));
    }

    private boolean insertNewDataObject (NodeObserver nodeObserver, P2PDataObject dataObject) {
        if (objectsKnownAlready.add(ByteBuffer.wrap(dataObject.getHash()))) {
            addNewDataObjectToMap(nodeObserver, dataObject);
            objectsInsertedSinceLastBroadcast++;
            return true;
        }
        return false;
    }

    private void broadcast () {
        if (shouldBroadcastCurrentData()) {
            updateObservers();
        }
    }

    private void updateObservers () {
        for (NodeObserver observer : observerList) {
            List<P2PDataObject> objectList = dataObjectMap.get(observer);
            observer.update(objectList);
        }
    }

    private boolean shouldBroadcastCurrentData () {
        return objectsInsertedSinceLastBroadcast > 10;
    }

    private void addNewDataObjectToMap (NodeObserver nodeObserver, P2PDataObject dataObject) {
        for (NodeObserver nodeObserver1 : observerList) {
            if (nodeObserver != nodeObserver1) {
                dataObjectMap.get(nodeObserver1).add(dataObject);
            }
        }
    }
}
