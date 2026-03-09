import { useParams, useNavigate } from "react-router-dom";
import { leaveRandomChat } from "../api/randomApi";
import ChatRoomPage from "./ChatRoomPage";

function RandomChatRoomPage() {

  const { roomId } = useParams();
  const navigate = useNavigate();

  const leave = async () => {

    if (!roomId) return;

    await leaveRandomChat(Number(roomId));
    navigate("/random");

  };

  return (
    <div>

      <div style={{ padding: 10 }}>
        <button onClick={leave}>채팅 종료</button>
      </div>

      <ChatRoomPage isRandom={true} />

    </div>
  );
}

export default RandomChatRoomPage;