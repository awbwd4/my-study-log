import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { apiClient } from "../api/client";
import { useAuth, type UserType } from "./AuthContext";

export function CompleteProfilePage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [type, setType] = useState<UserType>("STUDENT");
  const [name, setName] = useState("");
  const [academyName, setAcademyName] = useState("");
  const [phone, setPhone] = useState("");
  const [kakaoOpenChatLink, setKakaoOpenChatLink] = useState("");
  const [error, setError] = useState<string | null>(null);

  const tempToken = sessionStorage.getItem("tempToken");

  if (!tempToken) {
    navigate("/login");
    return null;
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    try {
      const { data } = await apiClient.post("/api/auth/register-profile", {
        tempToken,
        type,
        name,
        academyName: type === "TEACHER" ? academyName : undefined,
        phone: type === "STUDENT" ? phone : undefined,
        kakaoOpenChatLink: type === "STUDENT" ? kakaoOpenChatLink : undefined,
      });
      sessionStorage.removeItem("tempToken");
      login(data.token, data.type);
      navigate("/");
    } catch {
      setError("프로필 등록 중 오류가 발생했습니다");
    }
  };

  return (
    <div style={{ maxWidth: 360, margin: "80px auto", padding: 16 }}>
      <h1>추가 정보 입력</h1>
      <form onSubmit={handleSubmit}>
        <select value={type} onChange={(e) => setType(e.target.value as UserType)} style={{ width: "100%", marginBottom: 8 }}>
          <option value="STUDENT">학생</option>
          <option value="TEACHER">강사</option>
        </select>
        <input
          placeholder="이름"
          value={name}
          onChange={(e) => setName(e.target.value)}
          required
          style={{ width: "100%", marginBottom: 8 }}
        />
        {type === "TEACHER" ? (
          <input
            placeholder="학원명"
            value={academyName}
            onChange={(e) => setAcademyName(e.target.value)}
            required
            style={{ width: "100%", marginBottom: 8 }}
          />
        ) : (
          <>
            <input
              placeholder="전화번호"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              required
              style={{ width: "100%", marginBottom: 8 }}
            />
            <input
              placeholder="카카오톡 단톡방 링크 (선택)"
              value={kakaoOpenChatLink}
              onChange={(e) => setKakaoOpenChatLink(e.target.value)}
              style={{ width: "100%", marginBottom: 8 }}
            />
          </>
        )}
        {error && <p style={{ color: "red" }}>{error}</p>}
        <button type="submit" style={{ width: "100%", padding: 8 }}>
          시작하기
        </button>
      </form>
    </div>
  );
}
