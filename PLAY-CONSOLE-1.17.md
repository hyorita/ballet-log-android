# Ballet Log 1.17 — 업로드 자료

versionCode 21 / versionName 1.17. iOS 1.17(build 1) 동기화.

1.16.2로 잡았다가 할로윈 아이콘 하나 외에 발레 페르소나 저장 + Stats 비교까지
묵직한 기능이 더 붙어서 **사용자 결정으로 1.17로 올림** (1.16.1도 "기능 하나"로
패치 버전을 썼던 전례와 비교해 이번 건 그보다 큼). iOS도 동일하게 1.16.2 →
1.17로 bump.

빌드 산출물:
- 원스토어: `app/build/outputs/apk/release/BalletLog-1.17.apk`
- Play Store: `app/build/outputs/bundle/release/app-release.aab`

```bash
./gradlew :app:assembleRelease :app:bundleRelease   # APK·AAB 같은 시점에 같이
```

---

## 1.17 Release Notes (Play Store / 원스토어 공용)

스토어 "이번 버전의 새로운 기능" 필드에 그대로 붙여넣기. iOS `WHATS-NEW-1.17.md`와 같은 문구.
할로윈 아이콘은 윈도우(10/25~31)가 이미 지나서 Release Notes에서 뺌 — 지금 언급하면
바로 안 보여서 혼란만 줌. 코드는 들어있고 내년 할로윈부터 정상 동작.

### 한국어

```
✨ 1.17
• 발레 페르소나 결과 저장 — 퀴즈 결과 화면에서 "앱에 저장"을 누르면 설정에서 언제든 다시 볼 수 있어요.
• 통계에 이전 기간 대비 변화가 표시돼요.
```

### English

```
✨ 1.17
• Find My Ballet Persona now saves your result — tap "Save to app" on the quiz result page and see it anytime in Settings.
• Stats now shows how this period compares to the last one.
```

### 日本語

```
✨ 1.17
• バレエペルソナの結果を保存 — 診断結果画面の「アプリに保存」をタップすると、設定からいつでも確認できます。
• 統計に前の期間との比較が表示されるようになりました。
```

---

## 1.17 변경사항 (개발자 메모)

세 기능 번들 (iOS와 동일 커밋 순서):

1. **할로윈 대체 앱 아이콘** — 이미 포팅 완료(1.16.2 작업분, `7f7bdc5` 등). 윈도우
   10/25~31, 지금은 휴면. 변경 없음, 그대로 포함.
2. **발레 페르소나 결과를 앱으로 되가져오기** — `MainActivity`의
   `balletlog://persona` intent-filter + `MbtiPreferences`가 `type`/`name`/`image`
   세 파라미터 저장, Settings 행이 아랫줄 서브타이틀 + 썸네일로 표시(`1795e1d`).
   iOS와 달리 **Custom Tabs는 "닫히지 않는" 문제 자체가 없음** — 딥링크 타면
   Android가 알아서 MainActivity를 포그라운드로 올리고 탭은 백그라운드로 감.
3. **Stats 이전 기간 비교** — `StatsMath.kt`의 `comparisonValueText` +
   `StatsViewModel`의 `computeRange(period, offset - 1, now)` 재사용(`bd69a11`).
   6개 단위 테스트로 iOS와 동일 엣지케이스 검증.

**Room 스키마 변경 없음. 새 권한 없음.**

---

## 출시 전 확인

- [x] App Store·Google Play·원스토어 모두 제출 완료 (2026-11-11), 승인 대기 중
- [x] release AAB/APK 빌드 — 2026-11-10 16:41, versionCode 21 확인
- [x] 개인정보처리방침 URL — 변경 없음
- [x] 데이터 세이프티 섹션 — 변경 없음 (앱이 새로 수집하는 데이터 없음)

> versionCode 21은 어떤 트랙에든 업로드되는 순간 영구 점유됨. 이전 19/20은 실제
> 업로드 여부 불확실 — 확인 안 하고 21로 안전하게 올림.

---

## 원스토어 업로드 절차

1. 원스토어 개발자 센터 → 앱 관리 → Ballet Log → 새 버전 등록
2. APK 업로드: `BalletLog-1.17.apk` (versionCode 21)
3. 위 "1.17 Release Notes" 한국어 텍스트 붙여넣기
4. 심사 제출

## Play Store 업로드

Play Console → Production 트랙 → Create new release → AAB 업로드 → Release notes 붙여넣기.
