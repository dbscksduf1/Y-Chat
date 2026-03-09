import axios from "./axios";

export const createPrivateRoom = async (friendEmail: string) => {
  const res = await axios.post(
    `/api/chat/rooms/private?friendUsername=${friendEmail}`
  );
  return res.data;
};

export const getMyChatRooms = async () => {
  const res = await axios.get("/api/chat/rooms");
  return res.data;
};