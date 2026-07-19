/**
 * 업로드 전에 사진을 리사이즈+압축한다. 폰 카메라 원본(수 MB, 4000px 이상)을 그대로 올리면
 * 느리고 서버 요청 크기 제한에도 쉽게 걸리는데, 텍스트 인식(OCR)에는 그 정도 해상도가
 * 필요 없다. 긴 변 기준 2000px, JPEG 품질 80%면 화질은 충분히 유지하면서 용량은 크게 줄어든다.
 *
 * imageOrientation: "from-image"는 세로로 찍은 사진이 EXIF 회전 정보를 무시하고 옆으로
 * 눕는 흔한 버그를 방지한다 (캔버스는 기본적으로 EXIF를 반영하지 않는다).
 */
export async function compressImage(file: File, maxDimension = 2000, quality = 0.8): Promise<File> {
  if (!file.type.startsWith("image/")) return file;

  try {
    const bitmap = await createImageBitmap(file, { imageOrientation: "from-image" });
    const scale = Math.min(1, maxDimension / Math.max(bitmap.width, bitmap.height));
    const targetWidth = Math.round(bitmap.width * scale);
    const targetHeight = Math.round(bitmap.height * scale);

    const canvas = document.createElement("canvas");
    canvas.width = targetWidth;
    canvas.height = targetHeight;
    const ctx = canvas.getContext("2d");
    if (!ctx) return file;
    ctx.drawImage(bitmap, 0, 0, targetWidth, targetHeight);
    bitmap.close();

    const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, "image/jpeg", quality));
    if (!blob || blob.size >= file.size) return file;

    const newName = file.name.replace(/\.[^./]+$/, "") + ".jpg";
    return new File([blob], newName, { type: "image/jpeg" });
  } catch {
    // 압축 실패 시 원본 그대로 업로드 (안전망)
    return file;
  }
}
