import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

export function Nav() {
  const { type, logout } = useAuth();
  const navigate = useNavigate();

  return (
    <nav style={{ display: "flex", gap: 16, padding: 12, borderBottom: "1px solid #ddd", alignItems: "center" }}>
      <Link to="/">오답노트</Link>
      {type === "TEACHER" && <Link to="/classes">반 관리</Link>}
      <span style={{ marginLeft: "auto", fontSize: 12, color: "#888" }}>{type === "TEACHER" ? "강사" : "학생"}</span>
      <button
        onClick={() => {
          logout();
          navigate("/login");
        }}
      >
        로그아웃
      </button>
    </nav>
  );
}
