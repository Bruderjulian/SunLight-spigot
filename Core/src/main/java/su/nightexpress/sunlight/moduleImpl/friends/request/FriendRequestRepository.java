package su.nightexpress.sunlight.moduleImpl.friends.request;

import su.nightexpress.nightcore.user.UserInfo;
import su.nightexpress.sunlight.moduleImpl.friends.model.FriendRequest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The pending request queue, in memory only.
 * <p>
 * Reads drop expired entries as a side effect, so a list taken from here may be shorter than it was
 * a moment ago. That is the same contract the PTP request queue offers, and it is why the module
 * never treats a stale entry as a refusal reason.
 */
public class FriendRequestRepository {

    private final Map<UUID, List<FriendRequest>> incomingByTarget;
    private final Map<UUID, List<FriendRequest>> outgoingBySender;

    public FriendRequestRepository() {
        this.incomingByTarget = new ConcurrentHashMap<>();
        this.outgoingBySender = new ConcurrentHashMap<>();
    }

    public void clear() {
        this.incomingByTarget.clear();
        this.outgoingBySender.clear();
    }

    private List<FriendRequest> incoming(UUID targetId) {
        List<FriendRequest> list = this.incomingByTarget.computeIfAbsent(targetId, key -> new ArrayList<>());
        synchronized (list) {
            list.removeIf(FriendRequest::isExpired);
        }
        return list;
    }

    private List<FriendRequest> outgoing(UUID senderId) {
        List<FriendRequest> list = this.outgoingBySender.computeIfAbsent(senderId, key -> new ArrayList<>());
        synchronized (list) {
            list.removeIf(FriendRequest::isExpired);
        }
        return list;
    }

    public List<FriendRequest> getIncoming(UUID targetId) {
        List<FriendRequest> list = new ArrayList<>(this.incoming(targetId));
        list.sort(Comparator.comparing(FriendRequest::getExpireDate));
        return list;
    }

    public List<FriendRequest> getOutgoing(UUID senderId) {
        List<FriendRequest> list = new ArrayList<>(this.outgoing(senderId));
        list.sort(Comparator.comparing(FriendRequest::getExpireDate));
        return list;
    }

    public int countIncoming(UUID targetId) {
        return this.incoming(targetId).size();
    }

    public int countOutgoing(UUID senderId) {
        return this.outgoing(senderId).size();
    }

    /** How many requests that player is involved in, from either end. */
    public int countTotal(UUID playerId) {
        return this.countIncoming(playerId) + this.countOutgoing(playerId);
    }

    public FriendRequest getIncoming(UUID targetId, UUID senderId) {
        return this.incoming(targetId).stream()
                .filter(request -> request.getSenderId().equals(senderId))
                .findFirst()
                .orElse(null);
    }

    public FriendRequest getOutgoing(UUID senderId, UUID targetId) {
        return this.outgoing(senderId).stream()
                .filter(request -> request.getTargetId().equals(targetId))
                .findFirst()
                .orElse(null);
    }

    /** The request between those two in that direction, or {@code null}. */
    public FriendRequest get(UUID senderId, UUID targetId) {
        return this.getOutgoing(senderId, targetId);
    }

    /** Whether that pair already has a request in either direction, which blocks a second one. */
    public boolean hasBetween(UUID playerId, UUID otherId) {
        return this.getIncoming(playerId, otherId) != null || this.getIncoming(otherId, playerId) != null;
    }

    public void add(FriendRequest request) {
        this.removeBetween(request.getSenderId(), request.getTargetId());

        this.incoming(request.getTargetId()).add(request);
        this.outgoing(request.getSenderId()).add(request);
    }

    public void remove(FriendRequest request) {
        this.removeBetween(request.getSenderId(), request.getTargetId());
    }

    public void removeBetween(UUID senderId, UUID targetId) {
        List<FriendRequest> incoming = this.incomingByTarget.get(targetId);
        if (incoming != null) {
            synchronized (incoming) {
                incoming.removeIf(request -> request.getSenderId().equals(senderId));
            }
        }

        List<FriendRequest> outgoing = this.outgoingBySender.get(senderId);
        if (outgoing != null) {
            synchronized (outgoing) {
                outgoing.removeIf(request -> request.getTargetId().equals(targetId));
            }
        }
    }

    /** Drops everything that player has sent or received, on quit. */
    public void clearFor(UUID playerId) {
        List<FriendRequest> outgoing = this.outgoingBySender.remove(playerId);
        if (outgoing != null) {
            synchronized (outgoing) {
                outgoing.forEach(request -> this.removeIncoming(request));
            }
        }

        List<FriendRequest> incoming = this.incomingByTarget.remove(playerId);
        if (incoming != null) {
            synchronized (incoming) {
                incoming.forEach(request -> this.removeOutgoing(request));
            }
        }
    }

    private void removeIncoming(FriendRequest request) {
        List<FriendRequest> list = this.outgoingBySender.get(request.getSenderId());
        if (list == null) {
            return;
        }
        synchronized (list) {
            list.removeIf(entry -> entry.getTargetId().equals(request.getTargetId()));
        }
    }

    private void removeOutgoing(FriendRequest request) {
        List<FriendRequest> list = this.incomingByTarget.get(request.getTargetId());
        if (list == null) {
            return;
        }
        synchronized (list) {
            list.removeIf(entry -> entry.getSenderId().equals(request.getSenderId()));
        }
    }

    /** Whether that player has an unexpired request from that sender. */
    public boolean hasIncoming(UUID targetId, UUID senderId) {
        return this.getIncoming(targetId, senderId) != null;
    }

    /** Names of the players who sent that player a request, for command suggestions. */
    public List<String> incomingNames(UUID targetId) {
        return this.getIncoming(targetId).stream().map(FriendRequest::getSenderName).toList();
    }

    public List<String> outgoingNames(UUID senderId) {
        return this.getOutgoing(senderId).stream().map(FriendRequest::getTargetName).toList();
    }

    public static UserInfo other(FriendRequest request, UUID playerId) {
        return request.getSenderId().equals(playerId) ? request.getTarget() : request.getSender();
    }
}