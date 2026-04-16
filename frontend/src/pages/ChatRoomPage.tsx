import { useEffect, useState, useRef } from "react";
import { useParams, useNavigate } from "react-router-dom";
import axios from "../api/axios";
import { Client } from "@stomp/stompjs";
import { getMyChatRooms, getMessages } from "../api/chatApi";

interface ChatMessage {
  id: number | null;
  roomId: number | null;
  sender: string;
  content: string;
  createdAt: string;
  profileImageUrl: string | null;
  unreadCount: number;
  system?: boolean;
}



(window as any).global = window;


function ChatRoomPage({ isRandom }: { isRandom?: boolean }) {

    const [cursorId, setCursorId] = useState<number | null>(null);
    const [hasMore, setHasMore] = useState(true);
  

  const { roomId } = useParams();
  const navigate = useNavigate();

  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState("");

  const stompClient = useRef<Client | null>(null);
  const messageEndRef = useRef<HTMLDivElement | null>(null);

  const myEmail = sessionStorage.getItem("email");
  console.log("roomId:", roomId)

  useEffect(() => {

    if (!roomId) return;

    // 랜덤채팅이면 메시지 로딩 안함
    if (!isRandom) {
      loadMessages();
    }

    connect();

    return () => {

      if (stompClient.current?.connected && !isRandom) {

        stompClient.current.publish({
          destination: "/app/chat.leave",
          body: JSON.stringify({
            roomId: Number(roomId),
          }),
        });

      }

      if (stompClient.current) {
        stompClient.current.deactivate();
      }

    };

  }, [roomId]);

  useEffect(() => {
    messageEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  useEffect(() => {
    setCursorId(null);
    setHasMore(true);
    setMessages([]);
  }, [roomId]);



      const loadMessages = async () => {
        if (!hasMore) return;

        try {
          const res = await getMessages(Number(roomId), cursorId ?? undefined);
          const newMessages = res;

          if (newMessages.length === 0) {
            setHasMore(false);
            return;
          }

          // 첫 로딩 vs 추가 로딩 구분
          if (cursorId === null) {
            setMessages(newMessages);
          } else {
            setMessages((prev) => [...newMessages, ...prev]);
          }

          setCursorId(newMessages[0].id);

        } catch (err) {
          console.error("메시지 불러오기 실패", err);
        }
      };


  const handleScroll = (e: any) => {
    if (e.target.scrollTop === 0) {
      loadMessages();
    }
  };


  const connect = () => {

    const token = sessionStorage.getItem("token");

    const client = new Client({

      webSocketFactory: () =>
        new WebSocket(`${import.meta.env.VITE_WS_URL}/ws-chat`),

      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },

      reconnectDelay: 5000,

      onConnect: () => {

        client.subscribe(`/topic/room/${roomId}`, (message) => {

          const newMessage = JSON.parse(message.body);

if (newMessage.sender === "system") {
  newMessage.system = true;
}

setMessages((prev) => [...prev, newMessage]);

        });

        // 일반 채팅에서만 읽음 처리
        if (!isRandom) {

          client.subscribe(`/user/queue/read`, (message) => {

            const data = JSON.parse(message.body);

            setMessages((prev) =>
              prev.map((msg) =>
                msg.id === data.messageId
                  ? { ...msg, unreadCount: 0 }
                  : msg
              )
            );

          });

          client.subscribe(`/user/queue/unread`, async () => {
            await getMyChatRooms();
          });

        }

        // 일반 채팅에서만 enter 이벤트
        if (!isRandom) {
          client.publish({
            destination: "/app/chat.enter",
            body: JSON.stringify({
              roomId: Number(roomId),
            }),
          });
        }

      },
    });

    client.activate();
    stompClient.current = client;

  };

  const handleSend = () => {

    if (!input.trim()) return;
    if (!stompClient.current?.connected) return;

    stompClient.current.publish({
      destination: "/app/chat.send",
      body: JSON.stringify({
        roomId: Number(roomId),
        content: input,
      }),
    });

    setInput("");

  };

  const handleLeaveRoom = async () => {

    if (!window.confirm("채팅방을 나가시겠습니까?")) return;

    // 랜덤채팅이면 그냥 이동
    if (isRandom) {

  stompClient.current?.publish({
    destination: "/app/chat.random.leave",
    body: JSON.stringify({
      roomId: Number(roomId)
    })
  });

  navigate("/random");
  return;
}

    try {

      await axios.delete(`/api/chat/rooms/${roomId}/leave`);

      alert("채팅방에서 나갔습니다");

      navigate("/main");

    } catch (err) {

      alert("채팅방 나가기 실패");

    }

  };

  const handleInvite = async () => {

    const username = prompt("초대할 사용자 이메일 입력");

    if (!username) return;

    try {

      await axios.post(
        `/api/chat/rooms/${roomId}/invite`,
        null,
        { params: { username } }
      );

      alert("초대 완료");

    } catch (err) {

      alert("초대 실패");

    }

  };

  return (

    <div style={styles.container}>

      <div style={styles.topBar}>

        {!isRandom && (
          <button onClick={handleInvite}>초대</button>
        )}

        <button onClick={handleLeaveRoom}>나가기</button>

      </div>

      <div style={styles.messageArea} onScroll={handleScroll}>

        {messages.map((msg, index) => {
          if (msg.system) {
  return (
    <div key={index} style={styles.systemMessage}>
      {msg.content}
    </div>
  );
}
          

          const isMine = msg.sender === myEmail;

          return (

            <div
              key={index}
              style={{
                display: "flex",
                justifyContent: isMine ? "flex-end" : "flex-start",
              }}
            >

              {!isMine && (
                <img
                  src={
                    isRandom || !msg.profileImageUrl
                      ? "https://cdn-icons-png.flaticon.com/512/847/847969.png"
                      : `${import.meta.env.VITE_API_BASE_URL}${msg.profileImageUrl}`
                  }
                  style={styles.avatar}
                />
              )}

              <div style={styles.messageColumn}>

                {!isMine && (
                  <div style={styles.sender}>
  {isRandom ? "익명의 사용자" : msg.sender}
</div>
                )}

                <div style={styles.messageRow}>

                  {isMine && msg.unreadCount > 0 && (
                    <div style={styles.readCount}>
                      {msg.unreadCount}
                    </div>
                  )}

                  <div
                    style={{
                      ...styles.bubble,
                      backgroundColor: isMine ? "#d9fdd3" : "white",
                    }}
                  >
                    {msg.content}
                  </div>

                </div>

                <div style={styles.time}>
                  {new Date(msg.createdAt + "Z").toLocaleTimeString("ko-KR", {
                    hour: "2-digit",
                    minute: "2-digit"
                  })}

                </div>

              </div>

            </div>

          );

        })}

        <div ref={messageEndRef} />

      </div>

      <div style={styles.inputArea}>

        <input
          style={styles.input}
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="메시지를 입력하세요"
        />

        <button
          style={styles.button}
          onClick={handleSend}
        >
          전송
        </button>

      </div>

    </div>

  );

}

const styles:any = {

container:{
height:"100vh",
display:"flex",
flexDirection:"column"
},

topBar:{
display:"flex",
gap:10,
padding:10,
borderBottom:"1px solid #ddd"
},

messageArea:{
flex:1,
overflowY:"auto",
padding:10,
background:"#f5f5f5",
display:"flex",
flexDirection:"column",
gap:10
},

avatar:{
width:32,
height:32,
borderRadius:"50%",
marginRight:8
},

messageColumn:{
display:"flex",
flexDirection:"column",
maxWidth:"70%"
},

sender:{
fontSize:12,
color:"gray",
marginBottom:2
},

messageRow:{
display:"flex",
alignItems:"flex-end"
},

bubble:{
padding:"10px 12px",
borderRadius:12,
boxShadow:"0 1px 2px rgba(0,0,0,0.1)"
},

readCount:{
fontSize:11,
color:"red",
marginRight:4,
fontWeight:"bold"
},

time:{
fontSize:10,
color:"gray",
marginTop:2
},

inputArea:{
display:"flex",
padding:10,
borderTop:"1px solid #ddd"
},

input:{
flex:1,
padding:8
},

button:{
marginLeft:10,
padding:"8px 16px",
background:"#3b82f6",
color:"white",
border:"none",
borderRadius:6
},
systemMessage: {
  textAlign: "center",
  color:"#777",
  fontSize:13,
  margin:"10px 0"
}

};

export default ChatRoomPage;