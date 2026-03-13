import { useEffect, useState } from "react";
import axios from "../api/axios";
import { useNavigate } from "react-router-dom";

interface ChatRoom {
  roomId: number;
  roomName: string;
  lastMessage: string;
  lastMessageTime: string;
  unreadCount: number;
}

function ChatListPage() {

  const [rooms, setRooms] = useState<ChatRoom[]>([]);
  const navigate = useNavigate();

  useEffect(() => {
    loadRooms();
  }, []);

  const loadRooms = async () => {
    try {
      const res = await axios.get("/api/chat/rooms");
      setRooms(res.data);
    } catch (err) {src={selectedFriend?.profileImageUrl || DEFAULT_PROFILE}
      console.error(err);
    }
  };

  const enterRoom = (roomId: number) => {
    navigate(`/chat/${roomId}`);
  };

  return (
    <div style={styles.container}>
      <h2 style={styles.title}>채팅</h2>

      {rooms.map((room) => (
        <div
          key={room.roomId}
          style={styles.room}
          onClick={() => enterRoom(room.roomId)}
        >
          <div style={styles.roomTop}>
  <div style={styles.name}>
    {room.roomName
      .split("_")
      .find(
        (name: string) =>
          name !== (sessionStorage.getItem("nickname") || "")
      ) || room.roomName}
  </div>

  <div style={styles.time}>
    {room.lastMessageTime
      ? new Date(room.lastMessageTime).toLocaleTimeString([], {
          hour: "2-digit",
          minute: "2-digit",
        })
      : ""}
  </div>
</div>

<div style={styles.roomBottom}>
  <div style={styles.message}>{room.lastMessage}</div>

  {room.unreadCount > 0 && (
    <div style={styles.unread}>{room.unreadCount}</div>
  )}
</div>
        </div>
      ))}
    </div>
  );
}

const styles: any = {

  container: {
    padding: 16,
  },

  title: {
    fontSize: 20,
    marginBottom: 20,
  },

  room: {
    borderBottom: "1px solid #eee",
    padding: "12px 0",
    cursor: "pointer",
  },

  roomTop: {
    display: "flex",
    justifyContent: "space-between",
    marginBottom: 6,
  },

  name: {
    fontWeight: "bold",
  },

  time: {
    fontSize: 12,
    color: "#999",
  },

  roomBottom: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "center",
  },

  message: {
    color: "#666",
    fontSize: 14,
  },

  unread: {
    backgroundColor: "#ff3b30",
    color: "white",
    borderRadius: 12,
    padding: "2px 8px",
    fontSize: 12,
  },

};

export default ChatListPage;