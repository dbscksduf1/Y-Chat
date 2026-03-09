import { useState } from "react";
import axios from "../api/axios";
import { useNavigate } from "react-router-dom";
import { BsChatDotsFill } from "react-icons/bs";

function LoginPage() {

  const navigate = useNavigate();

  const [isSignup, setIsSignup] = useState(false);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [nickname, setNickname] = useState("");

  const handleSubmit = async () => {
    try {
      if (isSignup) {
        await axios.post("/api/auth/signup", {
          email,
          password,
          nickname,
        });
        alert("회원가입 성공! 로그인 해주세요.");
        setIsSignup(false);
      } else {
        const res = await axios.post("/api/auth/login", {
          email,
          password,
        });

        sessionStorage.setItem("token", res.data.token);
        sessionStorage.setItem("email", res.data.email);
        sessionStorage.setItem("nickname", res.data.nickname);

        navigate("/main");
      }
    } catch (err: any) {
      alert("에러 발생: " + (err.response?.data || "서버 오류"));
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900">

      {/* 로그인 카드 */}
      <div className="w-[360px] bg-slate-800 text-white rounded-2xl shadow-xl p-8">

        {/* 로고 */}
        <div className="flex items-center justify-center mb-4 gap-2">

  <BsChatDotsFill className="text-blue-400 text-3xl"/>

  <h1 className="text-2xl font-bold tracking-widest">
    Y Chat
  </h1>

</div>

        <h2 className="text-center text-lg font-semibold mb-5">
          {isSignup ? "회원가입" : "로그인"}
        </h2>

        {/* 이메일 */}
        <input
          className="w-full mb-3 px-4 py-2 rounded-lg bg-slate-700 border border-slate-600 focus:outline-none focus:border-blue-400"
          placeholder="이메일"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />

        {/* 비밀번호 */}
        <input
          type="password"
          className="w-full mb-3 px-4 py-2 rounded-lg bg-slate-700 border border-slate-600 focus:outline-none focus:border-blue-400"
          placeholder="비밀번호"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />

        {/* 닉네임 */}
        {isSignup && (
          <input
            className="w-full mb-3 px-4 py-2 rounded-lg bg-slate-700 border border-slate-600 focus:outline-none focus:border-blue-400"
            placeholder="닉네임"
            value={nickname}
            onChange={(e) => setNickname(e.target.value)}
          />
        )}

        {/* 버튼 */}
        <button
          onClick={handleSubmit}
          className="w-full bg-blue-500 hover:bg-blue-600 transition py-2 rounded-lg font-semibold mt-2"
        >
          {isSignup ? "회원가입" : "로그인"}
        </button>

        {/* 하단 링크 */}
        <p
          className="text-center text-sm text-slate-400 mt-5 cursor-pointer hover:text-white"
          onClick={() => setIsSignup(!isSignup)}
        >
          {isSignup
            ? "이미 계정이 있으신가요? 로그인"
            : "회원가입 하러가기"}
        </p>

      </div>

    </div>
  );
}

export default LoginPage;