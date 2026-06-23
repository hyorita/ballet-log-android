# Ballet Log 1.10 — 업로드 자료

versionCode 11 / versionName 1.10

빌드 산출물:
- 원스토어: `app/build/outputs/apk/release/BalletLog-1.10.apk`
- Play Store: `./gradlew :app:bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`

iOS 1.10 동기화. Log 그리드 정돈 — 사진 없는 워크아웃을 월별 하단 strip으로 분리.

---

## 1.10 Release Notes (Play Store / 원스토어 공용)

짧은 버전. 스토어 "이번 버전의 새로운 기능" 필드에 그대로 붙여넣기.

### 한국어

```
✨ 1.10
• 더 깔끔해진 Log 그리드 — 아직 사진이 없는 워크아웃은 매월 아래쪽 줄로 모입니다. 사진이 그리드의 주인공이 되고, 운동 기록은 단정하게 정리돼요.
• 워크아웃에 사진을 추가하면 자동으로 사진 콜라주에 합류합니다.
```

### English

```
✨ 1.10
• Tidier Log grid — workouts that don't have a photo yet are gathered into a clean strip at the bottom of each month, so your photos lead the grid.
• Add a photo to a workout and it rejoins the photo collage automatically.
```

### 日本語

```
✨ 1.10
• すっきりした Log グリッド — まだ写真のないワークアウトは、各月の下部にすっきりとした列としてまとまります。写真がグリッドの主役になります。
• ワークアウトに写真を追加すると、自動で写真コラージュに戻ります。
```

---

## 1.10 변경사항 (개발자 메모)

Log 그리드에서 사진 없는 워크아웃 placeholder를 콜라주 타일에서 빼내, 월별 하단 **strip band**(달력 날짜 블록 + 우측 정렬 kcal · min · bpm)로 묶음. 사진을 추가하면 더 이상 workout-only가 아니므로 콜라주에 자동 복귀. **Room 스키마 변경 없음(version 6 유지) → 백업 양방향 안전.**

| 항목 | 변경 |
|---|---|
| `PhotoLogScreen.kt` | `CollageGroup`에 `placeholders: List<PhotoLog>` 추가. 월별 `logs`를 photos(콜라주) / workout-only(strip) 분리, 콜라주 row는 photos만으로 빌드. 헤더 → 콜라주 row → strip band 순으로 렌더. |
| `PhotoLogScreen.kt` | 기존 그리드 타일 `WorkoutGridThumb` 제거 → 전폭 row `WorkoutStripCard` 신규 (iOS `WorkoutStripCard` 미러: 요일/일 블록 + kcal·min·bpm). `GridThumb`·`buildCollageRows`의 workout-only 분기 제거. |
| `build.gradle.kts` | versionCode 11, versionName 1.10. |

> 참고: iOS strip은 워크아웃 타입(Barre/Dance 등) 라벨을 표시하지만, Android는 워크아웃 타입을 저장한 적이 없어(`ScannedWorkout`/`PhotoLog` 모두 미보유) strip은 날짜 + kcal·min·bpm만 표시. 기존 Android 그리드 카드와 동일 데이터라 회귀 없음.

---

## 원스토어 업로드 절차

1. 원스토어 개발자 센터 → 앱 관리 → Ballet Log → 새 버전 등록
2. APK 업로드: `BalletLog-1.10.apk` (versionCode 11)
3. 위 "1.10 Release Notes" 한국어 텍스트 붙여넣기
4. 심사 제출

---

## Play Store 업로드

```bash
./gradlew :app:bundleRelease
# 출력: app/build/outputs/bundle/release/app-release.aab
```

Play Console → Production 트랙 → Create new release → AAB 업로드 → Release notes 붙여넣기.

> ⚠️ versionCode 11은 어떤 트랙에든 업로드되는 순간 영구 점유됨. 베타에서 버그 발견 시 재사용 불가 → 1.10.1 (versionCode 12)로 bump. (1.8 → 1.8.1 전례 참조)
