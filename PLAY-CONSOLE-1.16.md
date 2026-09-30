# Ballet Log 1.16 — 업로드 자료

versionCode 17 / versionName 1.16

빌드 산출물:
- 원스토어: `app/build/outputs/apk/release/BalletLog-1.16.apk`
- Play Store: `./gradlew :app:bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`

iOS 1.16 동기화. 탭 구조 개편 — Stats가 독립 탭(History 자리)으로 올라오며 전면
리디자인, History는 Class 탭 헤더의 캘린더 아이콘에서 여는 레이어로 이동.

> ⚠️ **동시 출시 예정.** iOS 1.16은 이 글 작성 시점(2026-09-30) 기준 `main` 머지·푸시만
> 완료, App Store 심사 제출 전. 아래 "출시 전 확인" 체크리스트를 반드시 먼저 볼 것.

---

## 1.16 Release Notes (Play Store / 원스토어 공용)

스토어 "이번 버전의 새로운 기능" 필드에 그대로 붙여넣기. (Play Console 언어당 500자 이내)

iOS `WHATS-NEW-1.16.md` 기준. 셋째 항목(Log 사진 배지 표시 버그 수정)은 iOS SwiftUI
전용 버그라 안드로이드에는 없어서 뺐다.

### 한국어

```
✨ 1.16
• 통계, 이제 독립 탭으로 — 연속 기록 카운터, 운동 요약, 트렌드 차트로 새로 디자인되어 한 번의 탭으로 바로 확인할 수 있습니다. 통계 화면을 이미지로 공유할 수도 있어요.
• 기록 위치 변경 — 클래스 탭 오른쪽 위 캘린더 아이콘에서 찾을 수 있습니다.
```

### English

```
✨ 1.16
• Stats gets its own tab — Redesigned with a streak counter, workout totals, and a trend chart, so your progress is one tap away instead of buried in History. Share it as an image, too.
• History moved — Find it from the calendar icon at the top right of the Class tab.
```

### 日本語

```
✨ 1.16
• 統計が独立したタブに — 連続記録カウンター、ワークアウトの合計、トレンドチャートを備えた新デザインで、ワンタップで確認できます。画像として共有することもできます。
• 履歴の場所が変わりました — クラスタブ右上のカレンダーアイコンからアクセスできます。
```

---

## 1.16 변경사항 (개발자 메모)

상세 규칙·결정 근거는 `PORT-1.16.md`.

| iOS 항목 | Android 대응 |
|---|---|
| **Stats 탭** (History 자리) | ✅ 포팅. 탭 5개 `로그·클래스·노트·통계·음악` |
| **History → Class 헤더 레이어** | ✅ 포팅. 단 **시트가 아니라 전체 화면 레이어** — History 안의 편집기 텍스트 입력이 M3 바텀시트와 IME에서 충돌. 앱의 다른 오버레이와 같은 방식 |
| **Stats 리디자인** | ✅ 포팅. 스트릭 히어로 · 2×2 · 트렌드 차트 1개 · By studio · 헤더 `+` · 공유 이미지 |
| **Stats "New" 배지** | ✅ 포팅, **판정 기준 변경** — 아래 |
| Log 그리드 배지 화면 밖 버그 | ⛔ 해당 없음 (SwiftUI 레이아웃 전용) |
| 카탈로그 v11 | ✅ 번들 시드 v8 → v11 |

**사용자 체감 변경 — 통계 수치가 iOS와 같아짐.** 같은 날 수업 기록과 Health Connect
운동(ID 없는 것)이 함께 있으면 예전엔 둘 다 셌는데, 이제 iOS처럼 수업 1회로 센다.
심박수만 있는 사진은 운동으로 세지 않는다. 기존 사용자 중 일부는 1.15보다 수업 수가
줄어 보일 수 있음 — 버그 아님. 문의 오면 이 설명으로 답할 것.

**New 배지 판정을 iOS와 다르게.** iOS는 "Log 탭 `+`를 누른 적 있는가"(튜토리얼 플래그)로
기존 사용자를 가르는데, 이 플래그는 백업 복원·Class 탭만 쓰는 사용자에겐 없다(실기기에서
확인). 안드로이드는 **기록이 하나라도 있는가**로 판정. iOS 1.16.x 반영 검토 필요.

**의도적으로 iOS와 다르게 둔 것:**
- 주 단위 = 일요일 시작 달력 주 (iOS는 최근 7일) — 기존 `이번 주` 라벨 유지
- 카드 색 = 앱의 다른 탭과 같은 톤 (iOS는 회색 배경 위 흰 카드)

**Room 스키마 변경 없음.** DAO에 `count()` 쿼리 3개만 추가(읽기 전용) — 마이그레이션 불필요.

**새 의존성 없음.** 공유 이미지는 Compose `GraphicsLayer` 캡처(기존 BOM 2024.09 범위).

**테스트:** 단위 40건 (`StatsCountingTest` 13 · `StatsMathTest` 16 신규). 실기기
SM-S926N / Android 16 (글꼴 배율 1.15) — 확인 범위는 `PORT-1.16.md` 상태 섹션.

---

## 출시 전 확인 (동시 출시라서 특히 중요)

- [ ] **iOS 1.16이 App Store 심사를 통과했는지** — 통과 전엔 Android도 올리지 않는다(동시 출시 합의)
- [ ] **릴리즈 빌드 실기기 실행** — debug로만 확인함. 공유 이미지(GraphicsLayer)가 R8 후에도
      정상인지, 통계 탭 첫 진입 확인
- [ ] 스트릭 진행 중 · 신기록 상태 화면 — 테스트 데이터에 없어 단위 테스트로만 확인됨
- [ ] 개인정보처리방침 URL — 변경 없음
- [ ] 데이터 세이프티 섹션 — 변경 없음(추가 권한/수집 없음)

---

## 원스토어 업로드 절차

1. 원스토어 개발자 센터 → 앱 관리 → Ballet Log → 새 버전 등록
2. APK 업로드: `BalletLog-1.16.apk` (versionCode 17)
3. 위 "1.16 Release Notes" 한국어 텍스트 붙여넣기
4. 심사 제출

---

## Play Store 업로드

```bash
./gradlew :app:assembleRelease :app:bundleRelease
# APK: app/build/outputs/apk/release/BalletLog-1.16.apk
# AAB: app/build/outputs/bundle/release/app-release.aab
```

Play Console → Production 트랙 → Create new release → AAB 업로드 → Release notes 붙여넣기.

> ⚠️ APK·AAB는 같은 시점에 같이 빌드할 것 (1.14 때 옛 AAB 재업로드로 "버전 코드가 이미
> 사용되었습니다" 오류 전례).
>
> versionCode 17은 어떤 트랙에든 업로드되는 순간 영구 점유됨. 베타에서 버그 발견 시
> 재사용 불가 → 1.16.1 (versionCode 18)로 bump.
