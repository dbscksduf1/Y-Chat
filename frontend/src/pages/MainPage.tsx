import { useState, useEffect, useRef } from "react";
import {
  getFriends,
  requestFriend,
  getPendingFriends,
  acceptFriend,
  getMyProfile,
  uploadProfileImage,
  updateProfile
} from "../api/friendApi";
import { createPrivateRoom, getMyChatRooms } from "../api/chatApi";
import { blockUser, getBlockedUsers, unblockUser } from "../api/blockApi";
import { useNavigate } from "react-router-dom";
import { Client } from "@stomp/stompjs";
import RandomChatPage from "./RandomChatPage";
const DEFAULT_PROFILE =
"https://cdn-icons-png.flaticon.com/512/847/847969.png";

function MainPage() {
  

  const navigate = useNavigate();
  const clientRef = useRef<Client | null>(null);

  const myEmail = sessionStorage.getItem("email");
  const token = sessionStorage.getItem("token");

  const [activeTab, setActiveTab] = useState<"friends" | "chats" | "random">("friends");

  const [friends, setFriends] = useState<any[]>([]);
  const [pending, setPending] = useState<any[]>([]);
  const [rooms, setRooms] = useState<any[]>([]);

  const [blocked, setBlocked] = useState<any[]>([]);
  const [showBlocked, setShowBlocked] = useState(false);

  const [me, setMe] = useState<any>(null);

  const [showAddFriend, setShowAddFriend] = useState(false);
  const [targetEmail, setTargetEmail] = useState("");
  const [showBlockMenu, setShowBlockMenu] = useState(false);
  const [showBlockAdd, setShowBlockAdd] = useState(false);
  const [blockEmail, setBlockEmail] = useState("");

  const [selectedFriend, setSelectedFriend] = useState<any>(null);

  const [showProfileEdit, setShowProfileEdit] = useState(false);
  const [statusMessage, setStatusMessage] = useState("");

  useEffect(() => {
    loadMyProfile();
    loadFriends();
    loadPending();
  }, []);

  useEffect(() => {
    if (activeTab === "chats") loadRooms();
  }, [activeTab]);

  useEffect(() => {

    const client = new Client({
      brokerURL: `wss://y-chat-my45.onrender.com/ws-chat?token=${token}`,
      reconnectDelay: 5000
    });

    client.onConnect = () => {

      client.subscribe("/user/queue/chatroom-update", () => {
        loadRooms();
      });

    };

    client.activate();
    clientRef.current = client;

    return () => client.deactivate();

  }, []);
  

  const loadMyProfile = async () => {

    const data = await getMyProfile();
    setMe(data);
    setStatusMessage(data.statusMessage || "");

  };

  const loadFriends = async () => {

    const data = await getFriends();
    console.log("친구목록:", data);

    setFriends(data);

  };
  const getNicknameByEmail = (email: string) => {
  const friend = friends.find(f => f.email === email);
  return friend ? friend.nickname : email;
};

  const loadPending = async () => {

    const data = await getPendingFriends();
    setPending(data);

  };

  const loadRooms = async () => {

  const data = await getMyChatRooms();

  const sorted = data.sort(
    (a:any, b:any) =>
      new Date(b.lastMessageTime).getTime() -
      new Date(a.lastMessageTime).getTime()
  );

  setRooms(sorted);

};

const loadBlocked = async () => {
  const data = await getBlockedUsers();
  console.log("차단목록 데이터:", data);
  setBlocked(data);
};
  const handleRequestFriend = async () => {

    await requestFriend(targetEmail);

    alert("친구 요청 완료");

    setTargetEmail("");
    setShowAddFriend(false);

  };

  const handleAccept = async (email: string) => {

    await acceptFriend(email);

    loadFriends();
    loadPending();

  };

  const handleStartChat = async (email: string) => {

    const roomId = await createPrivateRoom(email);

    navigate(`/rooms/${roomId}`);

  };

  const enterRoom = (roomId: number) => {

    navigate(`/rooms/${roomId}`);

  };
  const handleLogout = () => {

  sessionStorage.removeItem("token");
  sessionStorage.removeItem("email");
  sessionStorage.removeItem("nickname");

  navigate("/");

};

  const handleProfileUpload = async (e: any) => {

    const file = e.target.files[0];

    if (!file) return;

    await uploadProfileImage(file);

    loadMyProfile();

  };

  const handleSaveProfile = async () => {

  await updateProfile(statusMessage);

  alert("프로필 수정 완료");

  setShowProfileEdit(false);

};
  return (

    <div style={styles.container}>

      {activeTab === "friends" && me && (

  <div>

    <div style={styles.sectionTitleRow}>

  <div style={styles.sectionTitle}>
    내 프로필
  </div>

  <div
    style={styles.logoutText}
    onClick={handleLogout}
  >
    로그아웃
  </div>

</div>

    <div style={styles.myProfile}>

      <img
  src={me.profileImageUrl || DEFAULT_PROFILE}
  style={styles.avatar}
/>

      <div style={{ flex: 1 }}>

        <div>{me.nickname}</div>

        <div style={{ fontSize: 12, color: "gray" }}>
          {me.statusMessage || "상태메시지 없음"}
        </div>

      </div>

  <button
    style={styles.editBtn}
    onClick={() => setShowProfileEdit(true)}
  >
    수정
  </button>
  
  
  </div>
</div>

      )}

      {activeTab === "friends" && (

        <div style={styles.header}>

          <h3>친구 ({friends.length})</h3>

          <div>

            <button
              style={styles.iconBtn}
              onClick={() => {
                setShowBlockMenu(true);
              }}
            >
              🚫
            </button>

            <button
              style={styles.iconBtn}
              onClick={() => setShowAddFriend(true)}
            >
              +
            </button>

          </div>

        </div>

      )}

      <div style={styles.content}>

        {activeTab === "friends" && (

          <div>

            {pending.map((p, index) => (

              <div key={index} style={styles.friendItem}>

                <img
  src={p.profileImageUrl || DEFAULT_PROFILE}
  style={styles.avatar}
/>

                <div style={{ flex: 1 }}>{p.nickname}</div>

                <button
                  style={styles.acceptBtn}
                  onClick={() => handleAccept(p.email)}
                >
                  수락
                </button>

              </div>

            ))}

            {friends.map((f, index) => (

              <div
                key={index}
                style={styles.friendItem}
                onClick={() => setSelectedFriend(f)}
              >

                <div style={styles.profileWrapper}>

                  <img
  src={f.profileImageUrl || DEFAULT_PROFILE}
  style={styles.avatar}
/>

                  <div
                    style={{
                      ...styles.onlineDot,
                      backgroundColor: f.online ? "#00c73c" : "#999"
                    }}
                  />

                </div>

                <div style={{flex:1}}>

  <div style={{display:"flex",justifyContent:"space-between"}}>

    <div>{f.nickname}</div>

    {f.statusMessage && (
      <div style={{fontSize:12,color:"#888"}}>
        {f.statusMessage}
      </div>
    )}

  </div>

  {!f.online && (
    <div style={styles.lastSeen}>
      {f.lastSeen}
    </div>
  )}

</div>

              </div>

            ))}

          </div>

        )}

        {activeTab === "chats" && (

<div>

<div style={styles.sectionTitle}>
채팅방
</div>

            {rooms.map((room) => (

              <div
                key={room.roomId}
                style={styles.chatItem}
                onClick={() => enterRoom(room.roomId)}
              >

                <img
  src={DEFAULT_PROFILE}
  style={styles.avatar}
/>

                <div style={{ flex: 1 }}>

                  <div style={styles.chatTop}>

                    <div>
{getNicknameByEmail(
  room.roomName
    .split("_")
    .find((name:string)=>name!==sessionStorage.getItem("email"))
)}
</div>

                    <div style={styles.chatTime}>
                      {room.lastMessageTime
                        ? new Date(room.lastMessageTime).toLocaleTimeString([],{
                            hour:'2-digit',
                            minute:'2-digit'
                          })
                        : ""}
                    </div>

                  </div>

                  <div style={styles.chatBottom}>

                    <div style={{fontSize:13,color:"#666"}}>
                      {room.lastMessage}
                    </div>

                    {room.unreadCount > 0 && (
                      <div style={styles.unread}>
                        {room.unreadCount}
                      </div>
                    )}

                  </div>

                </div>

              </div>

            ))}

          </div>

        )}

      {activeTab === "random" && (
    <RandomChatPage />
  )}

</div>

      {/* 친구추가 모달 */}
      {showAddFriend && (
        <div style={styles.modal}>
          <div style={styles.modalCard}>
            <h3>친구 추가</h3>
            <input
              placeholder="이메일"
              value={targetEmail}
              onChange={(e)=>setTargetEmail(e.target.value)}
            />
            <button onClick={handleRequestFriend}>요청</button>
            <button onClick={()=>setShowAddFriend(false)}>닫기</button>
          </div>
        </div>
      )}

      {/* 차단 메뉴 */}
      {showBlockMenu && (
        <div style={styles.modal}>
          <div style={styles.modalCard}>
            <button onClick={()=>setShowBlockAdd(true)}>차단 추가</button>

            <button onClick={()=>{
              loadBlocked();
              setShowBlocked(true);
              setShowBlockMenu(false);
            }}>
              차단 목록
            </button>

            <button onClick={()=>setShowBlockMenu(false)}>닫기</button>
          </div>
        </div>
      )}

      {/* 차단 추가 */}
      {showBlockAdd && (
        <div style={styles.modal}>
          <div style={styles.modalCard}>
            <input
              placeholder="차단할 이메일"
              value={blockEmail}
              onChange={(e)=>setBlockEmail(e.target.value)}
            />
            <button
              onClick={async()=>{
                await blockUser(blockEmail);
                alert("차단 완료");
                setShowBlockAdd(false);
              }}
            >
              차단
            </button>
            <button onClick={()=>setShowBlockAdd(false)}>닫기</button>
          </div>
        </div>
      )}

      {/* 프로필 수정 */}
      {showProfileEdit && (
        <div style={styles.modal}>
          <div style={styles.modalCard}>
            <h3>프로필 수정</h3>
            <input
              value={statusMessage}
              onChange={(e)=>setStatusMessage(e.target.value)}
            />
            <input type="file" onChange={handleProfileUpload}/>
            <button onClick={handleSaveProfile}>저장</button>
            <button onClick={()=>setShowProfileEdit(false)}>닫기</button>
          </div>
        </div>
      )}

      {/* 차단 목록 */}
      {showBlocked && (
        <div style={styles.modal}>
          <div style={styles.modalCard}>

            <h3>차단 목록</h3>

            {blocked.length === 0 && (
              <div>차단된 유저 없음</div>
            )}

            {blocked.map((b:any, index:number) => (

              <div
                key={index}
                style={{
                  display:"flex",
                  justifyContent:"space-between",
                  alignItems:"center"
                }}
              >

                <div>{b.blockedUsername}</div>

                <button
                  onClick={async()=>{
                    await unblockUser(b.blockedUsername)
                    loadBlocked()
                  }}
                >
                  차단해제
                </button>

              </div>

            ))}

            <button onClick={()=>setShowBlocked(false)}>
              닫기
            </button>

          </div>
        </div>
      )}

      {selectedFriend && (
  <div style={styles.modal}>
    <div style={styles.modalCard}>

      <img
        src={selectedFriend.profileImageUrl || DEFAULT_PROFILE}
        style={{
          width:100,
          height:100,
          borderRadius:"50%",
          alignSelf:"center"
        }}
      />

      <h3 style={{textAlign:"center"}}>
        {selectedFriend.nickname}
      </h3>

      {!selectedFriend.online && (
        <div style={{textAlign:"center",fontSize:12,color:"#888"}}>
          {selectedFriend.lastSeen}
        </div>
      )}

      <button
        onClick={()=>{
          handleStartChat(selectedFriend.email)
          setSelectedFriend(null)
        }}
      >
        1대1 채팅
      </button>

      <button
        onClick={()=>setSelectedFriend(null)}
      >
        닫기
      </button>

    </div>
  </div>
)}

      <div style={styles.tabBar}>

        <button
          style={activeTab === "friends" ? styles.activeTab : styles.tab}
          onClick={() => setActiveTab("friends")}
        >
          친구
        </button>

        <button
          style={activeTab === "chats" ? styles.activeTab : styles.tab}
          onClick={() => setActiveTab("chats")}
        >
          채팅
        </button>

        <button
  style={activeTab === "random" ? styles.activeTab : styles.tab}
  onClick={() => setActiveTab("random")}
>
  랜덤채팅
</button>

        

      </div>

    </div>

  );

}





const styles:any = {

container:{width:430,margin:"0 auto",background:"white",height:"100vh",display:"flex",flexDirection:"column"},
header:{display:"flex",justifyContent:"space-between",alignItems:"center",padding:10,borderBottom:"1px solid #eee"},
content:{flex:1,overflowY:"auto"},
friendItem:{display:"flex",alignItems:"center",padding:15,borderBottom:"1px solid #eee",cursor:"pointer"},
chatItem:{display:"flex",alignItems:"center",padding:15,borderBottom:"1px solid #eee",cursor:"pointer"},

chatTop:{
display:"flex",
justifyContent:"space-between",
marginBottom:3
},

chatTime:{
fontSize:11,
color:"#888"
},

avatar:{width:40,height:40,borderRadius:"50%",background:"#ddd",marginRight:10},
acceptBtn:{background:"#3b82f6",color:"white",border:"none",padding:"5px 10px"},
iconBtn:{border:"none",background:"none",fontSize:18,marginLeft:10,cursor:"pointer"},
tabBar:{display:"flex",borderTop:"1px solid #ddd"},
tab:{flex:1,border:"none",background:"white",padding:10},
activeTab:{flex:1,border:"none",background:"#eee",padding:10},
myProfile:{display:"flex",alignItems:"center",padding:15,borderBottom:"1px solid #eee"},
editBtn:{border:"1px solid #ddd",background:"white",padding:"5px 10px"},
modal:{position:"fixed",top:0,left:0,width:"100%",height:"100%",background:"rgba(0,0,0,0.4)",display:"flex",justifyContent:"center",alignItems:"center"},
modalCard:{background:"white",padding:20,borderRadius:8,display:"flex",flexDirection:"column",gap:10},
chatBottom:{
display:"flex",
justifyContent:"space-between",
alignItems:"center"
},
unread:{
background:"#ff3b30",
color:"white",
fontSize:12,
minWidth:18,
height:18,
borderRadius:9,
display:"flex",
alignItems:"center",
justifyContent:"center",
padding:"0 6px"
},

profileWrapper:{
position:"relative",
width:40,
height:40,
marginRight:10
},

onlineDot:{
position:"absolute",
bottom:0,
right:0,
width:10,
height:10,
borderRadius:"50%",
border:"2px solid white"
},

lastSeen:{
fontSize:12,
color:"#888"
},
sectionTitle:{
fontSize:14,
fontWeight:600,
padding:"12px 16px",
borderBottom:"1px solid #eee"
},
sectionTitleRow:{
display:"flex",
justifyContent:"space-between",
alignItems:"center",
padding:"10px 0"
},

logoutText:{
fontSize:14,
color:"#333",
cursor:"pointer"
}

};

export default MainPage;