import axios from "./axios";

export async function enterRandomChat() {
  const res = await axios.post("/api/random/enter");
  return res.data;
}

export async function cancelRandomChat() {
  const res = await axios.post("/api/random/cancel");
  return res.data;
}

export async function leaveRandomChat(roomId: number) {
  const res = await axios.post(`/api/random/leave/${roomId}`);
  return res.data;
}