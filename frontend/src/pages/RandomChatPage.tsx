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

    if (res.status === "WAIT") {
      setWaiting(true);
    }

    if (res.status === "MATCHED") {
      navigate("/random-room/" + res.roomId);
    }

  };

  const cancel = async () => {

    await cancelRandomChat();
    setWaiting(false);

  };

  return (
    <div style={styles.container}>

      {!waiting && (

        <div style={styles.card}>

          <div style={styles.icon}>💬</div>

          <h2 style={styles.title}>
            랜덤채팅
          </h2>

          <p style={styles.desc}>
            새로운 사람과 익명으로 대화해보세요
          </p>

          <button
            style={styles.startBtn}
            onClick={startRandomChat}
          >
            랜덤채팅 시작하기
          </button>

        </div>

      )}

      {waiting && (

        <div style={styles.card}>

          <div style={styles.spinner}></div>

          <h2 style={styles.title}>
            상대방 찾는중...
          </h2>

          <p style={styles.desc}>
            잠시만 기다려 주세요
          </p>

          <button
            style={styles.cancelBtn}
            onClick={cancel}
          >
            취소
          </button>

        </div>

      )}

    </div>
  );
}

const styles:any = {

  container:{
    display:"flex",
    justifyContent:"center",
    alignItems:"center",
    height:"100%",
    padding:20
  },

  card:{
    width:"100%",
    maxWidth:300,
    background:"white",
    borderRadius:12,
    padding:30,
    boxShadow:"0 4px 15px rgba(0,0,0,0.1)",
    textAlign:"center"
  },

  icon:{
    fontSize:40,
    marginBottom:10
  },

  title:{
    marginBottom:10
  },

  desc:{
    fontSize:13,
    color:"#666",
    marginBottom:20
  },

  startBtn:{
    width:"100%",
    padding:"12px 0",
    background:"#3b82f6",
    color:"white",
    border:"none",
    borderRadius:8,
    fontSize:15,
    cursor:"pointer"
  },

  cancelBtn:{
    width:"100%",
    padding:"10px 0",
    background:"#ef4444",
    color:"white",
    border:"none",
    borderRadius:8,
    cursor:"pointer"
  },

  spinner:{
    width:40,
    height:40,
    border:"4px solid #ddd",
    borderTop:"4px solid #3b82f6",
    borderRadius:"50%",
    margin:"0 auto 20px auto",
    animation:"spin 1s linear infinite"
  }

};

export default RandomChatPage;