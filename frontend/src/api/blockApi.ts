import axios from "./axios";

export const blockUser = async (username:string) => {
  await axios.post(`/api/users/block?username=${username}`);
};

export const getBlockedUsers = async () => {
  const res = await axios.get("/api/users/blocks");
  return res.data;
};

export const unblockUser = async (username:string) => {
  await axios.delete(`/api/users/block?username=${username}`);
};