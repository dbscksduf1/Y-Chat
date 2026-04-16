import { useState, useEffect } from "react";
import { enterRandomChat, cancelRandomChat } from "../api/randomApi";
import { useNavigate } from "react-router-dom";

function RandomChatPage() {
  const navigate = useNavigate();
  const [waiting, setWaiting] = useState(false);

  useEffect(() => {
    if (!waiting) return;
    const interval = setInterval(async () => {
      const res = await enterRandomChat();
      if (res.status === "MATCHED") {
        clearInterval(interval);
        navigate("/random-room/" + res.roomId);
      }
    }, 1500);
    return () => clearInterval(interval);
  }, [waiting, navigate]);

  const startRandomChat = async () => {
    const res = await enterRandomChat();
    if (res.status === "WAIT") setWaiting(true);
    if (res.status === "MATCHED") navigate("/random-room/" + res.roomId);
  };

  const cancel = async () => {
    await cancelRandomChat();
    setWaiting(false);
  };

  return (
    <div style={s.container}>
      <div style={s.card}>

        {!waiting ? (
          <>
            <div style={s.iconWrap}>
              <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="#3b82f6" strokeWidth="1.5">
                <polyline points="16 3 21 3 21 8"/>
                <line x1="4" y1="20" x2="21" y2="3"/>
                <polyline points="21 16 21 21 16 21"/>
                <line x1="15" y1="15" x2="21" y2="21"/>
              </svg>
            </div>
            <h2 style={s.title}>랜덤 채팅</h2>
            <p style={s.desc}>새로운 사람과 익명으로 대화해보세요</p>
            <button style={s.startBtn} onClick={startRandomChat}>
              시작하기
            </button>
          </>
        ) : (
          <>
            <div style={s.spinnerWrap}>
              <div style={s.spinner} />
            </div>
            <h2 style={s.title}>상대방 찾는 중</h2>
            <p style={s.desc}>잠시만 기다려 주세요...</p>
            <button style={s.cancelBtn} onClick={cancel}>취소</button>
          </>
        )}

      </div>

      <style>{`
        @keyframes spin { to { transform: rotate(360deg); } }
        @keyframes pulse { 0%,100% { opacity:1; } 50% { opacity:0.4; } }
      `}</style>
    </div>
  );
}

const s: any = {
  container: { display: "flex", justifyContent: "center", alignItems: "center", height: "100%", padding: 24, background: "#f0f2f5" },
  card: { width: "100%", maxWidth: 320, background: "white", borderRadius: 20, padding: "36px 28px", boxShadow: "0 4px 24px rgba(0,0,0,0.08)", textAlign: "center" },
  iconWrap: { width: 80, height: 80, background: "#eff6ff", borderRadius: "50%", display: "flex", alignItems: "center", justifyContent: "center", margin: "0 auto 20px" },
  title: { fontWeight: 700, fontSize: 20, color: "#111827", marginBottom: 8 },
  desc: { fontSize: 14, color: "#6b7280", marginBottom: 28, lineHeight: 1.5 },
  startBtn: { width: "100%", padding: "14px 0", background: "#3b82f6", color: "white", border: "none", borderRadius: 12, fontSize: 16, fontWeight: 600, cursor: "pointer" },
  spinnerWrap: { display: "flex", justifyContent: "center", marginBottom: 20 },
  spinner: { width: 48, height: 48, border: "4px solid #e5e7eb", borderTop: "4px solid #3b82f6", borderRadius: "50%", animation: "spin 0.9s linear infinite" },
  cancelBtn: { width: "100%", padding: "14px 0", background: "#fee2e2", color: "#ef4444", border: "none", borderRadius: 12, fontSize: 15, fontWeight: 600, cursor: "pointer" },
};

export default RandomChatPage;
