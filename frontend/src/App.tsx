import { BrowserRouter, Routes, Route } from "react-router-dom";
import { Suspense, lazy } from "react";
import LoginPage from "./pages/LoginPage";
import MainPage from "./pages/MainPage";
import RandomChatPage from "./pages/RandomChatPage";
import RandomChatRoomPage from "./pages/RandomChatRoomPage";

const ChatRoomPage = lazy(() => import("./pages/ChatRoomPage"));
const ChatListPage = lazy(() => import("./pages/ChatListPage"));

function App() {
  return (
    <BrowserRouter>
      <div style={styles.appContainer}>
        <div style={styles.appInner}>
          <Suspense fallback={<div style={{ padding: 20 }}>로딩중...</div>}>
            <Routes>
              <Route path="/" element={<LoginPage />} />
              <Route path="/main" element={<MainPage />} />

              <Route path="/chat" element={<ChatListPage />} />

              <Route path="/rooms/:roomId" element={<ChatRoomPage />} />
              <Route path="/random" element={<RandomChatPage />} />
<Route path="/random-room/:roomId" element={<RandomChatRoomPage />} />
            </Routes>
          </Suspense>
        </div>
      </div>
    </BrowserRouter>
  );
}

const styles: any = {
  appContainer: {
    display: "flex",
    justifyContent: "center",
    backgroundColor: "#f0f2f5",
    minHeight: "100vh",
  },
  appInner: {
    width: 430,
    backgroundColor: "white",
    minHeight: "100vh",
    boxShadow: "0 0 10px rgba(0,0,0,0.1)",
  },
};

export default App;