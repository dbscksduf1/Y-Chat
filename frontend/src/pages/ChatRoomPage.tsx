import { useEffect, useState, useRef } from "react";
import { useParams, useNavigate } from "react-router-dom";
import axios from "../api/axios";
import { Client } from "@stomp/stompjs";
import { getMyChatRooms, getMessages } from "../api/chatApi";

const API_BASE = import.meta.env.VITE_API_BASE_URL;
const WS_URL = import.meta.env.VITE_WS_URL;
const DEFAULT_PROFILE = "https://cdn-icons-png.flaticon.com/512/847/847969.png";

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

function ChatRoomPage({ isRandom }: { isRandom?: boolean }) {
  const [cursorId, setCursorId] = useState<number | null>(null);
  const [hasMore, setHasMore] = useState(true);

  const { roomId } = useParams();
  const navigate = useNavigate();

  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState("");

  const stompClient = useRef<Client | null>(null);
  const messageEndRef = useRef<HTMLDivElement | null>(null);
  const textareaRef = useRef<HTMLTextAreaElement | null>(null);

  const myEmail = sessionStorage.getItem("email");

  useEffect(() => {
    if (!roomId) return;
    if (!isRandom) loadMessages();
    connect();
    return () => {
      if (stompClient.current?.connected && !isRandom) {
        stompClient.current.publish({
          destination: "/app/chat.leave",
          body: JSON.stringify({ roomId: Number(roomId) }),
        });
      }
      if (stompClient.current) stompClient.current.deactivate();
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
      const newMessages = await getMessages(Number(roomId), cursorId ?? undefined);
      if (newMessages.length === 0) { setHasMore(false); return; }
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
    if (e.target.scrollTop === 0) loadMessages();
  };

  const connect = () => {
    const token = sessionStorage.getItem("token");
    const client = new Client({
      webSocketFactory: () => new WebSocket(`${WS_URL}/ws-chat`),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe(`/topic/room/${roomId}`, (message) => {
          const newMessage = JSON.parse(message.body);
          if (newMessage.sender === "system") newMessage.system = true;
          setMessages((prev) => [...prev, newMessage]);
        });
        if (!isRandom) {
          client.subscribe(`/user/queue/read`, (message) => {
            const data = JSON.parse(message.body);
            setMessages((prev) =>
              prev.map((msg) => msg.id === data.messageId ? { ...msg, unreadCount: 0 } : msg)
            );
          });
          client.subscribe(`/user/queue/unread`, async () => {
            await getMyChatRooms();
          });
          client.publish({
            destination: "/app/chat.enter",
            body: JSON.stringify({ roomId: Number(roomId) }),
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
      body: JSON.stringify({ roomId: Number(roomId), content: input }),
    });
    setInput("");
    if (textareaRef.current) textareaRef.current.style.height = "40px";
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleLeaveRoom = async () => {
    if (!window.confirm("채팅방을 나가시겠습니까?")) return;
    if (isRandom) {
      stompClient.current?.publish({
        destination: "/app/chat.random.leave",
        body: JSON.stringify({ roomId: Number(roomId) }),
      });
      navigate("/main");
      return;
    }
    try {
      await axios.delete(`/api/chat/rooms/${roomId}/leave`);
      navigate("/main");
    } catch {
      alert("채팅방 나가기 실패");
    }
  };

  const handleInvite = async () => {
    const username = prompt("초대할 사용자 이메일 입력");
    if (!username) return;
    try {
      await axios.post(`/api/chat/rooms/${roomId}/invite`, null, { params: { username } });
      alert("초대 완료");
    } catch {
      alert("초대 실패");
    }
  };

  const formatTime = (createdAt: string) => {
    return new Date(createdAt + "Z").toLocaleTimeString("ko-KR", {
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  return (
    <div style={s.root}>

      {/* 헤더 */}
      <div style={s.header}>
        <button style={s.backBtn} onClick={() => navigate("/main")}>
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
            <path d="M15 18l-6-6 6-6"/>
          </svg>
        </button>
        <span style={s.headerTitle}>{isRandom ? "랜덤 채팅" : "채팅"}</span>
        <div style={{ display: "flex", gap: 4 }}>
          {!isRandom && (
            <button style={s.headerBtn} onClick={handleInvite} title="초대">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/>
                <circle cx="9" cy="7" r="4"/>
                <line x1="19" y1="8" x2="19" y2="14"/>
                <line x1="22" y1="11" x2="16" y2="11"/>
              </svg>
            </button>
          )}
          <button style={s.headerBtn} onClick={handleLeaveRoom} title="나가기">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
              <polyline points="16 17 21 12 16 7"/>
              <line x1="21" y1="12" x2="9" y2="12"/>
            </svg>
          </button>
        </div>
      </div>

      {/* 메시지 영역 */}
      <div style={s.messageArea} onScroll={handleScroll}>

        {hasMore && (
          <div style={s.loadMoreHint}>위로 스크롤하여 이전 메시지 보기</div>
        )}

        {messages.map((msg, index) => {
          if (msg.system) {
            return (
              <div key={index} style={s.systemMsg}>{msg.content}</div>
            );
          }

          const isMine = msg.sender === myEmail;

          return (
            <div key={index} style={{ ...s.msgRow, justifyContent: isMine ? "flex-end" : "flex-start" }}>

              {!isMine && (
                <img
                  src={
                    isRandom || !msg.profileImageUrl
                      ? DEFAULT_PROFILE
                      : `${API_BASE}${msg.profileImageUrl}`
                  }
                  style={s.msgAvatar}
                />
              )}

              <div style={{ display: "flex", flexDirection: "column", alignItems: isMine ? "flex-end" : "flex-start", maxWidth: "68%" }}>

                {!isMine && (
                  <span style={s.senderName}>
                    {isRandom ? "익명의 사용자" : msg.sender}
                  </span>
                )}

                <div style={{ display: "flex", alignItems: "flex-end", gap: 5, flexDirection: isMine ? "row-reverse" : "row" }}>

                  {isMine && msg.unreadCount > 0 && (
                    <span style={s.unreadBadge}>{msg.unreadCount}</span>
                  )}

                  <div style={{ ...s.bubble, background: isMine ? "#3b82f6" : "#ffffff", color: isMine ? "#ffffff" : "#111827" }}>
                    {msg.content}
                  </div>

                </div>

                <span style={s.msgTime}>{formatTime(msg.createdAt)}</span>

              </div>

            </div>
          );
        })}

        <div ref={messageEndRef} />
      </div>

      {/* 입력 영역 */}
      <div style={s.inputArea}>
        <textarea
          ref={textareaRef}
          style={s.input}
          value={input}
          onChange={(e) => {
            setInput(e.target.value);
            e.target.style.height = "40px";
            e.target.style.height = Math.min(e.target.scrollHeight, 120) + "px";
          }}
          onKeyDown={handleKeyDown}
          placeholder="메시지를 입력하세요"
          rows={1}
        />
        <button
          style={{ ...s.sendBtn, background: input.trim() ? "#3b82f6" : "#e5e7eb" }}
          onClick={handleSend}
          disabled={!input.trim()}
        >
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke={input.trim() ? "white" : "#9ca3af"} strokeWidth="2.5">
            <line x1="22" y1="2" x2="11" y2="13"/>
            <polygon points="22 2 15 22 11 13 2 9 22 2"/>
          </svg>
        </button>
      </div>

    </div>
  );
}

const s: any = {
  root: { height: "100vh", display: "flex", flexDirection: "column", background: "#f0f2f5" },

  header: { display: "flex", alignItems: "center", padding: "10px 12px", background: "#ffffff", borderBottom: "1px solid #f3f4f6", gap: 4 },
  backBtn: { background: "none", border: "none", cursor: "pointer", padding: 6, color: "#374151", display: "flex", alignItems: "center", borderRadius: 8 },
  headerTitle: { flex: 1, fontWeight: 700, fontSize: 17, color: "#111827", paddingLeft: 4 },
  headerBtn: { background: "none", border: "none", cursor: "pointer", padding: 6, color: "#374151", display: "flex", alignItems: "center", borderRadius: 8 },

  messageArea: { flex: 1, overflowY: "auto", padding: "12px 16px", display: "flex", flexDirection: "column", gap: 4 },

  loadMoreHint: { textAlign: "center", fontSize: 12, color: "#9ca3af", padding: "4px 0 8px" },

  systemMsg: { textAlign: "center", fontSize: 12, color: "#9ca3af", background: "rgba(0,0,0,0.06)", borderRadius: 12, padding: "4px 12px", alignSelf: "center", margin: "6px 0" },

  msgRow: { display: "flex", alignItems: "flex-end", gap: 8, marginBottom: 2 },
  msgAvatar: { width: 34, height: 34, borderRadius: "50%", objectFit: "cover", flexShrink: 0, marginBottom: 2 },
  senderName: { fontSize: 12, color: "#6b7280", marginBottom: 3, paddingLeft: 2 },
  bubble: { padding: "9px 13px", borderRadius: 18, fontSize: 15, lineHeight: 1.4, boxShadow: "0 1px 2px rgba(0,0,0,0.08)", maxWidth: "100%", wordBreak: "break-word" },
  unreadBadge: { fontSize: 11, color: "#f59e0b", fontWeight: 700, marginBottom: 2, flexShrink: 0 },
  msgTime: { fontSize: 11, color: "#9ca3af", marginTop: 3, paddingLeft: 2, paddingRight: 2 },

  inputArea: { display: "flex", alignItems: "flex-end", padding: "10px 12px", background: "#ffffff", borderTop: "1px solid #f3f4f6", gap: 8 },
  input: { flex: 1, padding: "10px 14px", border: "1px solid #e5e7eb", borderRadius: 22, fontSize: 15, outline: "none", resize: "none", lineHeight: 1.4, height: 40, maxHeight: 120, overflowY: "auto", background: "#f9fafb", boxSizing: "border-box" },
  sendBtn: { width: 40, height: 40, borderRadius: "50%", border: "none", cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0, transition: "background 0.15s" },
};

export default ChatRoomPage;
