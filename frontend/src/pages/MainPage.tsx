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

const DEFAULT_PROFILE = "https://cdn-icons-png.flaticon.com/512/847/847969.png";
const API_BASE = import.meta.env.VITE_API_BASE_URL;
const WS_URL = import.meta.env.VITE_WS_URL;

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
      webSocketFactory: () => new WebSocket(`${WS_URL}/ws-chat`),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
    });
    client.onConnect = () => {
      client.subscribe("/user/queue/chatroom-update", () => loadRooms());
    };
    client.activate();
    clientRef.current = client;
    return () => { client.deactivate(); };
  }, []);

  const loadMyProfile = async () => {
    const data = await getMyProfile();
    setMe(data);
    setStatusMessage(data.statusMessage || "");
  };

  const loadFriends = async () => {
    const data = await getFriends();
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
      (a: any, b: any) =>
        new Date(b.lastMessageTime).getTime() - new Date(a.lastMessageTime).getTime()
    );
    setRooms(sorted);
  };

  const loadBlocked = async () => {
    const data = await getBlockedUsers();
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
    await loadMyProfile();
  };

  const handleSaveProfile = async () => {
    await updateProfile(statusMessage);
    alert("프로필 수정 완료");
    setShowProfileEdit(false);
  };

  const getRoomDisplayName = (roomName: string) => {
    const parts = roomName.split("_");
    const other = parts.find((p: string) => p !== myEmail);
    return other ? getNicknameByEmail(other) : roomName;
  };

  const getRoomAvatar = (roomName: string) => {
    const other = roomName.split("_").find((p: string) => p !== myEmail);
    const friend = friends.find(f => f.email === other);
    return friend?.profileImageUrl ? `${API_BASE}${friend.profileImageUrl}` : DEFAULT_PROFILE;
  };

  return (
    <div style={s.root}>

      {/* 상단 헤더 */}
      <div style={s.header}>
        <span style={s.headerTitle}>
          {activeTab === "friends" ? "친구" : activeTab === "chats" ? "채팅" : "랜덤채팅"}
        </span>
        {activeTab === "friends" && (
          <div style={s.headerActions}>
            <button style={s.iconBtn} onClick={() => setShowBlockMenu(true)} title="차단 관리">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="12" cy="12" r="10"/><line x1="4.93" y1="4.93" x2="19.07" y2="19.07"/>
              </svg>
            </button>
            <button style={s.iconBtn} onClick={() => setShowAddFriend(true)} title="친구 추가">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/>
                <circle cx="9" cy="7" r="4"/>
                <line x1="19" y1="8" x2="19" y2="14"/>
                <line x1="22" y1="11" x2="16" y2="11"/>
              </svg>
            </button>
          </div>
        )}
      </div>

      {/* 콘텐츠 */}
      <div style={s.content}>

        {/* 친구 탭 */}
        {activeTab === "friends" && (
          <div>
            {/* 내 프로필 */}
            {me && (
              <div style={s.myProfile}>
                <div style={s.avatarWrap}>
                  <img
                    src={me.profileImageUrl ? `${API_BASE}${me.profileImageUrl}` : DEFAULT_PROFILE}
                    style={s.avatarLg}
                  />
                </div>
                <div style={{ flex: 1 }}>
                  <div style={s.myName}>{me.nickname}</div>
                  <div style={s.myStatus}>{me.statusMessage || "상태메시지를 입력해보세요"}</div>
                </div>
                <div style={{ display: "flex", flexDirection: "column", gap: 6, alignItems: "flex-end" }}>
                  <button style={s.editBtn} onClick={() => setShowProfileEdit(true)}>수정</button>
                  <button style={s.logoutBtn} onClick={handleLogout}>로그아웃</button>
                </div>
              </div>
            )}

            {/* 친구 요청 */}
            {pending.length > 0 && (
              <div>
                <div style={s.sectionLabel}>친구 요청 {pending.length}</div>
                {pending.map((p, i) => (
                  <div key={i} style={s.listItem}>
                    <img src={p.profileImageUrl || DEFAULT_PROFILE} style={s.avatar} />
                    <div style={{ flex: 1 }}>
                      <div style={s.itemName}>{p.nickname}</div>
                      <div style={s.itemSub}>{p.email}</div>
                    </div>
                    <button style={s.acceptBtn} onClick={() => handleAccept(p.email)}>수락</button>
                  </div>
                ))}
              </div>
            )}

            {/* 친구 목록 */}
            <div style={s.sectionLabel}>친구 {friends.length}</div>
            {friends.map((f, i) => (
              <div key={i} style={s.listItem} onClick={() => setSelectedFriend(f)}>
                <div style={s.avatarWrap}>
                  <img
                    src={f.profileImageUrl ? `${API_BASE}${f.profileImageUrl}` : DEFAULT_PROFILE}
                    style={s.avatar}
                  />
                  <div style={{ ...s.onlineDot, background: f.online ? "#22c55e" : "#d1d5db" }} />
                </div>
                <div style={{ flex: 1 }}>
                  <div style={s.itemName}>{f.nickname}</div>
                  <div style={s.itemSub}>
                    {f.online ? "온라인" : f.lastSeen || f.statusMessage || ""}
                  </div>
                </div>
              </div>
            ))}
            {friends.length === 0 && (
              <div style={s.empty}>친구를 추가해보세요</div>
            )}
          </div>
        )}

        {/* 채팅 탭 */}
        {activeTab === "chats" && (
          <div>
            {rooms.map((room) => (
              <div key={room.roomId} style={s.listItem} onClick={() => enterRoom(room.roomId)}>
                <img src={getRoomAvatar(room.roomName)} style={s.avatar} />
                <div style={{ flex: 1, overflow: "hidden" }}>
                  <div style={s.chatTop}>
                    <span style={s.itemName}>{getRoomDisplayName(room.roomName)}</span>
                    <span style={s.timeText}>
                      {room.lastMessageTime
                        ? new Date(room.lastMessageTime + "Z").toLocaleTimeString("ko-KR", {
                            hour: "2-digit", minute: "2-digit",
                          })
                        : ""}
                    </span>
                  </div>
                  <div style={s.chatBottom}>
                    <span style={s.lastMsg}>{room.lastMessage}</span>
                    {room.unreadCount > 0 && (
                      <span style={s.badge}>{room.unreadCount}</span>
                    )}
                  </div>
                </div>
              </div>
            ))}
            {rooms.length === 0 && (
              <div style={s.empty}>채팅방이 없습니다</div>
            )}
          </div>
        )}

        {activeTab === "random" && <RandomChatPage />}
      </div>

      {/* 하단 탭바 */}
      <div style={s.tabBar}>
        {(["friends", "chats", "random"] as const).map((tab) => {
          const labels = { friends: "친구", chats: "채팅", random: "랜덤" };
          const icons = {
            friends: (
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                <circle cx="9" cy="7" r="4"/>
                <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
                <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
              </svg>
            ),
            chats: (
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
              </svg>
            ),
            random: (
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <polyline points="16 3 21 3 21 8"/>
                <line x1="4" y1="20" x2="21" y2="3"/>
                <polyline points="21 16 21 21 16 21"/>
                <line x1="15" y1="15" x2="21" y2="21"/>
              </svg>
            ),
          };
          const isActive = activeTab === tab;
          return (
            <button key={tab} style={{ ...s.tabBtn, color: isActive ? "#3b82f6" : "#9ca3af" }} onClick={() => setActiveTab(tab)}>
              {icons[tab]}
              <span style={{ fontSize: 11, marginTop: 2 }}>{labels[tab]}</span>
            </button>
          );
        })}
      </div>

      {/* 모달들 */}

      {showAddFriend && (
        <Modal title="친구 추가" onClose={() => setShowAddFriend(false)}>
          <input
            style={s.modalInput}
            placeholder="이메일 입력"
            value={targetEmail}
            onChange={(e) => setTargetEmail(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && handleRequestFriend()}
          />
          <button style={s.primaryBtn} onClick={handleRequestFriend}>요청 보내기</button>
        </Modal>
      )}

      {showBlockMenu && (
        <Modal title="차단 관리" onClose={() => setShowBlockMenu(false)}>
          <button style={s.primaryBtn} onClick={() => { setShowBlockAdd(true); setShowBlockMenu(false); }}>차단 추가</button>
          <button style={s.secondaryBtn} onClick={() => { loadBlocked(); setShowBlocked(true); setShowBlockMenu(false); }}>차단 목록 보기</button>
        </Modal>
      )}

      {showBlockAdd && (
        <Modal title="차단 추가" onClose={() => setShowBlockAdd(false)}>
          <input
            style={s.modalInput}
            placeholder="차단할 이메일"
            value={blockEmail}
            onChange={(e) => setBlockEmail(e.target.value)}
          />
          <button style={{ ...s.primaryBtn, background: "#ef4444" }} onClick={async () => {
            await blockUser(blockEmail);
            alert("차단 완료");
            setShowBlockAdd(false);
          }}>차단</button>
        </Modal>
      )}

      {showBlocked && (
        <Modal title="차단 목록" onClose={() => setShowBlocked(false)}>
          {blocked.length === 0
            ? <div style={{ textAlign: "center", color: "#9ca3af", padding: "16px 0" }}>차단된 유저 없음</div>
            : blocked.map((b: any, i: number) => (
              <div key={i} style={{ display: "flex", justifyContent: "space-between", alignItems: "center", padding: "8px 0", borderBottom: "1px solid #f3f4f6" }}>
                <span style={{ fontSize: 14 }}>{b.blockedUsername}</span>
                <button style={{ ...s.secondaryBtn, padding: "4px 12px", fontSize: 13 }} onClick={async () => {
                  await unblockUser(b.blockedUsername);
                  loadBlocked();
                }}>해제</button>
              </div>
            ))
          }
        </Modal>
      )}

      {showProfileEdit && (
        <Modal title="프로필 수정" onClose={() => setShowProfileEdit(false)}>
          <div style={{ textAlign: "center", marginBottom: 12 }}>
            <img
              src={me?.profileImageUrl ? `${API_BASE}${me.profileImageUrl}` : DEFAULT_PROFILE}
              style={{ width: 72, height: 72, borderRadius: "50%", objectFit: "cover" }}
            />
          </div>
          <label style={s.fileLabel}>
            프로필 사진 변경
            <input type="file" style={{ display: "none" }} onChange={handleProfileUpload} />
          </label>
          <input
            style={s.modalInput}
            placeholder="상태메시지"
            value={statusMessage}
            onChange={(e) => setStatusMessage(e.target.value)}
          />
          <button style={s.primaryBtn} onClick={handleSaveProfile}>저장</button>
        </Modal>
      )}

      {selectedFriend && (
        <Modal title="" onClose={() => setSelectedFriend(null)}>
          <div style={{ textAlign: "center", padding: "8px 0 16px" }}>
            <div style={{ ...s.avatarWrap, margin: "0 auto 10px", width: 72, height: 72 }}>
              <img
                src={selectedFriend.profileImageUrl ? `${API_BASE}${selectedFriend.profileImageUrl}` : DEFAULT_PROFILE}
                style={{ width: 72, height: 72, borderRadius: "50%", objectFit: "cover" }}
              />
              <div style={{ ...s.onlineDot, width: 14, height: 14, background: selectedFriend.online ? "#22c55e" : "#d1d5db" }} />
            </div>
            <div style={{ fontWeight: 700, fontSize: 17, marginBottom: 4 }}>{selectedFriend.nickname}</div>
            <div style={{ fontSize: 13, color: "#6b7280" }}>
              {selectedFriend.online ? "온라인" : selectedFriend.lastSeen || ""}
            </div>
          </div>
          <button style={s.primaryBtn} onClick={() => { handleStartChat(selectedFriend.email); setSelectedFriend(null); }}>
            1대1 채팅 시작
          </button>
        </Modal>
      )}

    </div>
  );
}

function Modal({ title, children, onClose }: { title: string; children: React.ReactNode; onClose: () => void }) {
  return (
    <div style={s.overlay} onClick={onClose}>
      <div style={s.modal} onClick={(e) => e.stopPropagation()}>
        <div style={s.modalHeader}>
          <span style={s.modalTitle}>{title}</span>
          <button style={s.closeBtn} onClick={onClose}>✕</button>
        </div>
        <div style={s.modalBody}>{children}</div>
      </div>
    </div>
  );
}

const s: any = {
  root: { width: "100%", height: "100vh", display: "flex", flexDirection: "column", background: "#ffffff" },
  header: { display: "flex", justifyContent: "space-between", alignItems: "center", padding: "16px 20px 12px", borderBottom: "1px solid #f3f4f6", background: "#ffffff" },
  headerTitle: { fontSize: 20, fontWeight: 700, color: "#111827" },
  headerActions: { display: "flex", gap: 4 },
  iconBtn: { background: "none", border: "none", cursor: "pointer", padding: 6, color: "#374151", display: "flex", alignItems: "center", borderRadius: 8 },
  content: { flex: 1, overflowY: "auto" },

  myProfile: { display: "flex", alignItems: "center", padding: "16px 20px", borderBottom: "1px solid #f3f4f6", gap: 12 },
  avatarLg: { width: 52, height: 52, borderRadius: "50%", objectFit: "cover" },
  myName: { fontWeight: 700, fontSize: 16, marginBottom: 3, color: "#111827" },
  myStatus: { fontSize: 13, color: "#9ca3af" },
  editBtn: { background: "#f3f4f6", border: "none", borderRadius: 6, padding: "4px 12px", fontSize: 12, cursor: "pointer", color: "#374151" },
  logoutBtn: { background: "none", border: "none", fontSize: 12, cursor: "pointer", color: "#9ca3af" },

  sectionLabel: { fontSize: 12, fontWeight: 600, color: "#9ca3af", padding: "12px 20px 6px", textTransform: "uppercase", letterSpacing: "0.05em" },

  listItem: { display: "flex", alignItems: "center", padding: "10px 20px", cursor: "pointer", gap: 12, transition: "background 0.1s" },
  avatarWrap: { position: "relative", flexShrink: 0 },
  avatar: { width: 46, height: 46, borderRadius: "50%", objectFit: "cover", background: "#e5e7eb" },
  onlineDot: { position: "absolute", bottom: 1, right: 1, width: 11, height: 11, borderRadius: "50%", border: "2px solid white" },
  itemName: { fontWeight: 600, fontSize: 15, color: "#111827", marginBottom: 2 },
  itemSub: { fontSize: 13, color: "#9ca3af" },
  acceptBtn: { background: "#3b82f6", color: "white", border: "none", borderRadius: 8, padding: "6px 14px", fontSize: 13, cursor: "pointer", flexShrink: 0 },
  empty: { textAlign: "center", color: "#9ca3af", padding: 40, fontSize: 14 },

  chatTop: { display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 3 },
  chatBottom: { display: "flex", justifyContent: "space-between", alignItems: "center" },
  lastMsg: { fontSize: 13, color: "#6b7280", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap", flex: 1 },
  timeText: { fontSize: 11, color: "#9ca3af", flexShrink: 0, marginLeft: 8 },
  badge: { background: "#ef4444", color: "white", fontSize: 11, minWidth: 18, height: 18, borderRadius: 9, display: "flex", alignItems: "center", justifyContent: "center", padding: "0 5px", flexShrink: 0, marginLeft: 6 },

  tabBar: { display: "flex", borderTop: "1px solid #f3f4f6", background: "#ffffff" },
  tabBtn: { flex: 1, border: "none", background: "none", padding: "10px 0 8px", cursor: "pointer", display: "flex", flexDirection: "column", alignItems: "center", gap: 2 },

  overlay: { position: "fixed", inset: 0, background: "rgba(0,0,0,0.4)", display: "flex", alignItems: "flex-end", justifyContent: "center", zIndex: 100 },
  modal: { background: "white", width: "100%", maxWidth: 430, borderRadius: "20px 20px 0 0", padding: "0 0 20px" },
  modalHeader: { display: "flex", justifyContent: "space-between", alignItems: "center", padding: "20px 20px 12px" },
  modalTitle: { fontWeight: 700, fontSize: 17, color: "#111827" },
  closeBtn: { background: "none", border: "none", fontSize: 18, cursor: "pointer", color: "#9ca3af", padding: 4 },
  modalBody: { padding: "0 20px", display: "flex", flexDirection: "column", gap: 10 },
  modalInput: { width: "100%", padding: "12px 14px", border: "1px solid #e5e7eb", borderRadius: 10, fontSize: 14, outline: "none", boxSizing: "border-box" },
  primaryBtn: { width: "100%", padding: "13px 0", background: "#3b82f6", color: "white", border: "none", borderRadius: 10, fontSize: 15, fontWeight: 600, cursor: "pointer" },
  secondaryBtn: { width: "100%", padding: "13px 0", background: "#f3f4f6", color: "#374151", border: "none", borderRadius: 10, fontSize: 15, cursor: "pointer" },
  fileLabel: { display: "block", textAlign: "center", padding: "10px 0", background: "#f3f4f6", borderRadius: 10, fontSize: 14, cursor: "pointer", color: "#374151" },
};

export default MainPage;
