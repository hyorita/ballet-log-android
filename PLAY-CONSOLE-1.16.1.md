# Ballet Log 1.16.1 — 업로드 자료

versionCode 18 / versionName 1.16.1

빌드 산출물:
- 원스토어: `app/build/outputs/apk/release/BalletLog-1.16.1.apk`
- Play Store: `app/build/outputs/bundle/release/app-release.aab`

```bash
./gradlew :app:assembleRelease :app:bundleRelease   # APK·AAB 같은 시점에 같이
```

iOS 1.16.1 동기화. balletclass.cc **"내 발레 페르소나 찾기"** 웹 퀴즈 진입점 —
설정 맨 위 행 · Log 탭 배너(첫 오픈 후 사라짐) · 기존 사용자용 New 표시.

> ⚠️ **동시 출시 예정.** iOS 1.16.1 (build 1)은 이 글 작성 시점(2026-10-15) 기준 로컬
> `main` 커밋만 완료, App Store 심사 제출 전. iOS 승인 후 업로드할 것.

---

## 1.16.1 Release Notes (Play Store / 원스토어 공용)

스토어 "이번 버전의 새로운 기능" 필드에 그대로 붙여넣기. iOS `WHATS-NEW-1.16.1.md`와 같은 문구
(기능 이름·설명은 앱 안 문구 그대로).

### 한국어

```
✨ 1.16.1
• 내 발레 페르소나 찾기 — 8문항으로 알아보는 나의 발레 클래스 성향 — 16가지 발레 페르소나 중 나는 누구? 로그 탭 위쪽이나 설정에서 바로 시작할 수 있어요.
```

### English

```
✨ 1.16.1
• Find My Ballet Persona — An 8-question quiz for your ballet class style — see which of 16 personas you are. Open it from the top of the Log tab or from Settings.
```

### 日本語

```
✨ 1.16.1
• 私のバレエペルソナ診断 — 8つの質問でわかるバレエクラスの傾向 — 16種類のバレエペルソナ、あなたはどのタイプ？ ログタブの上部または設定から始められます。
```

---

## 1.16.1 변경사항 (개발자 메모)

상세 규칙·결정 근거는 `PORT-1.16.1.md`.

| iOS 항목 | Android 대응 |
|---|---|
| 설정 "Find My Ballet Persona" 행 + New 캡슐 | ✅ 포팅 |
| Log 탭 배너 (첫 오픈 후 사라짐) | ✅ 포팅 — 그리드와 함께 스크롤, 태그 필터 위 |
| Log 톱니 빨간 점 | ✅ 포팅 — 1.16 Stats 배지와 같은 기존 사용자 판정 |
| SafariView (인앱 브라우저) | ✅ **Chrome Custom Tabs** |

**새 의존성 1개: `androidx.browser:browser:1.8.0`** (Custom Tabs). 데이터 수집·권한 변화 없음
— 링크를 여는 것뿐이고, 퀴즈 페이지의 GA 이벤트는 웹(balletclass.cc) 쪽 것.

**Room 스키마 변경 없음.** SharedPreferences 키 2개(`balletlog_mbti`)만 추가.

**퀴즈 URL의 `x7h=8aq6brde1s`** — 앱에서 온 방문자에게 결과 화면의 아트 스타일을 전부
열어주는 표시. 웹 `lib/mbti/appUnlock.js`가 원본이라 웹에서 바꾸면 iOS·Android 모두 같이
바꿔야 함.

---

## 출시 전 확인

- [ ] **iOS 1.16.1이 App Store 심사를 통과했는지** — 통과 전엔 Android도 올리지 않는다(동시 출시 합의)
- [ ] release AAB/APK 빌드 (위 명령, 버전 bump 후 반드시 새로 빌드)
- [ ] 개인정보처리방침 URL — 변경 없음
- [ ] 데이터 세이프티 섹션 — 변경 없음 (앱이 새로 수집하는 데이터 없음)

> versionCode 18은 어떤 트랙에든 업로드되는 순간 영구 점유됨. 버그 발견 시 1.16.2
> (versionCode 19)로 bump.

---

## 원스토어 업로드 절차

1. 원스토어 개발자 센터 → 앱 관리 → Ballet Log → 새 버전 등록
2. APK 업로드: `BalletLog-1.16.1.apk` (versionCode 18)
3. 위 "1.16.1 Release Notes" 한국어 텍스트 붙여넣기
4. 심사 제출

## Play Store 업로드

Play Console → Production 트랙 → Create new release → AAB 업로드 → Release notes 붙여넣기.
