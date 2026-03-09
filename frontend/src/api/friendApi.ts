import axios from "./axios";

// 친구 목록
export const getFriends = async () => {

  const res = await axios.get("/api/friends");

  return res.data;

};

// 친구 요청
export const requestFriend = async (friendEmail: string) => {

  const res = await axios.post(
    `/api/friends/request?friendEmail=${friendEmail}`
  );

  return res.data;

};

// 받은 친구 요청
export const getPendingFriends = async () => {

  const res = await axios.get("/api/friends/pending");

  return res.data;

};

// 친구 수락
export const acceptFriend = async (friendEmail: string) => {

  const res = await axios.post(
    `/api/friends/accept?friendEmail=${friendEmail}`
  );

  return res.data;

};



// 🔥 내 프로필 조회
export const getMyProfile = async () => {

  let nickname = sessionStorage.getItem("nickname");

  if (!nickname) {

    const email = sessionStorage.getItem("email");

    nickname = email?.split("@")[0] || "";

    sessionStorage.setItem("nickname", nickname);

  }

  const res = await axios.get("/api/users/profile", {
    params: { nickname }
  });

  return res.data;

};



// 🔥 상태메시지 수정
export const updateProfile = async (statusMessage: string) => {

  const res = await axios.patch(
    "/api/users/profile",
    {
      statusMessage
    }
  );

  return res.data;

};



// 🔥 프로필 이미지 업로드
export const uploadProfileImage = async (file: File) => {

  const formData = new FormData();

  formData.append("file", file);

  const res = await axios.post(
    "/api/users/profile-image",
    formData,
    {
      headers: {
        "Content-Type": "multipart/form-data"
      }
    }
  );

  return res.data;

};