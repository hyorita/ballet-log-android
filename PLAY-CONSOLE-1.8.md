# Ballet Log 1.8 — 업로드 자료

versionCode 8 / versionName 1.8

빌드 산출물:
- 원스토어: `app/build/outputs/apk/release/BalletLog-1.8.apk`
- Play Store: 필요 시 `./gradlew :app:bundleRelease` 로 AAB 생성

---

## 원스토어 1.8 Release Notes

### 한국어

```
✨ 1.8 업데이트
• Health Connect 자동 기록 — 워치/폰의 발레·필라테스·요가·근력 운동이 끝나면 칼로리·시간·심박수가 담긴 Photo Log가 자동 생성돼요. (24시간 내 앱 열면 동기화)
• 탭으로 사진 추가, 메뉴에서 제거 — 사진만 떼어내도 운동 정보는 그대로 유지됩니다.
• 편집기 자동 저장 — 기존 클래스를 편집하다 닫아도 변경사항이 저장돼요.
• 기본 스텝 이름 복수형 — Pliés, Tendus, Jetés, Fondus, Frappés, Développés.
```

---

## 1.8 변경사항 (개발자 메모)

iOS 1.8 기능 동기화. Apple Watch / Apple Health → Android Health Connect 대응.

| 항목 | 변경 |
|---|---|
| `HealthConnectAutoImport.kt` (신규) | 최근 24h `ExerciseSessionRecord` 스캔 → workout-only `PhotoLog` placeholder 삽입. `externalWorkoutId` 키로 재진입 dedupe. |
| `HealthConnectManager.kt` | `scanRecentWorkouts(lookbackHours)` 추가 — 발레 관련 ExerciseType만 필터. |
| `MainActivity.onStart()` | foreground 진입 시 60s throttle로 `HealthConnectAutoImport.importRecent` 호출 (notification 풀다운/잠금해제 시 매번 안 두드리도록). |
| `PhotoLog` + `MIGRATION_5_6` | `externalWorkoutId: String?` 컬럼 + 인덱스 추가 (Room version 5→6). |
| `PhotoLogDao.findByExternalWorkoutId` | dedupe 쿼리. |
| `PhotoLogViewModel.attachPhoto` / `removePhoto` | placeholder에 사진 첨부 / 사진만 제거 (워크아웃 정보 유지). |
| `PhotoLogCard.kt` | `WorkoutPlaceholderCard` 추가 (다크 차콜 + 큰 kcal + "+ Add Photo" 슬롯), `onWorkoutCardTap` 전체 탭 핸들러. |
| `PhotoLogScreen.kt` | 그리드용 `WorkoutGridThumb` (크림색 placeholder 카드). |
| `PhotoLogPager.kt` | `PickMultipleVisualMedia` 런처 + `onAttachPhoto`/`onRemovePhoto` 콜백, More 메뉴에 Remove Photo 추가 (사진이 있는 워크아웃 항목에서만 노출). |
| `SettingsScreen.kt` | Health Connect 섹션 — 연결 상태 / Connect 버튼 / "Open Health Connect" (인텐트 fallback → Play Store). |
| `HistoryScreen.kt` | Photo Log 카드 탭 → `PhotoLogPager` 사용 (share/edit/favorite/remove-photo/delete 모두 작동). 기존 `PhotoLogCard` 단독 표시 제거. |
| `EditorScreen.kt` (Class) | `DisposableEffect.onDispose` 에서 기존 클래스는 자동 저장. 신규 클래스는 hasMeaningfulContent 일 때만 insert. |
| `DefaultSteps.kt` | Barre 기본 스텝 복수형 (Pliés / Tendus / Jetés / Fondus / Frappés / Développés / Grand battements). |
| `strings.xml` (en/ko/ja) | `settings_health_connect*`, `photolog_add_photo`, `photolog_remove_photo`, `photolog_remove_photo_title/message`, `photolog_workout` 신규. |
| `build.gradle.kts` | versionCode 8, versionName 1.8. |

---

## 원스토어 업로드 절차

1. 원스토어 개발자 센터 → 앱 관리 → Ballet Log → 새 버전 등록
2. APK 업로드: `BalletLog-1.8.apk` (versionCode 8)
3. 위 "원스토어 1.8 Release Notes" 한국어 텍스트 붙여넣기
4. 심사 제출

---

## Play Store 업로드 (필요 시)

```bash
./gradlew :app:bundleRelease
# 출력: app/build/outputs/bundle/release/app-release.aab
```

Play Console → Production 트랙 → Create new release → AAB 업로드 → Release notes 붙여넣기.

### English

```
✨ What's new in 1.8
• Health Connect auto-logging — Barre, Dance, Pilates, Yoga, and Strength workouts auto-create a Photo Log entry with calories, duration, and heart rate. (Sync by opening the app within 24h.)
• Tap a workout card to attach a photo, remove it from the menu — your workout details stay either way.
• Editor auto-save — existing class log edits save when you close the editor.
• Plural default steps — Pliés, Tendus, Jetés, Fondus, Frappés, Développés.
```

### 日本語

```
✨ 1.8 アップデート
• Health Connect 自動連携 — バレエ・ピラティス・ヨガ・筋トレのワークアウトを終えると、消費カロリー・時間・心拍数が入った Photo Log が自動作成。(24時間以内にアプリを開いて同期)
• タップで写真追加、メニューで取り消し — 写真だけ外してもワークアウト情報は残ります。
• エディタの自動保存 — 既存クラスログは閉じた時点で自動保存。
• デフォルトステップ名を複数形に — Pliés, Tendus, Jetés, Fondus, Frappés, Développés。
```

---

## 출시 후 메모

- **마이그레이션**: Room 5→6, `photo_logs.externalWorkoutId TEXT NULL` + 인덱스 추가. 기존 사용자 데이터 그대로 유지.
- **Health Connect 미설치 기기**: Settings 섹션에서 "Health Connect is not available on this device." 안내 + auto-import는 silent no-op.
- **권한**: 최초 실행 시 `HealthConnectManager.permissions` 일괄 요청 (`PermissionController` ActivityResultContract). 거부해도 다른 기능 영향 없음.
- **동선 확인**:
  1. Health Connect 연결 후 워크아웃 종료 → 앱 진입 → Log 탭에 크림색 placeholder 카드 출현
  2. 카드 탭 → 풀스크린 다크 운동 카드 → 어디든 탭 또는 + Add Photo → 사진 선택 → 사진+운동 정보 합쳐진 풀스크린
  3. More(⋮) → 사진 모양 가린 아이콘(Remove Photo) → 확인 → 다시 placeholder 상태
  4. History 탭 → Photo Log 카드 탭 → Log 탭과 같은 풀스크린 + 모든 액션 사용 가능
  5. 기존 클래스 편집 → ✓ 안 누르고 뒤로/스와이프 → 자동 저장 확인
- **iOS와의 차이**: iOS는 HealthKit `WorkoutType` 기반, Android는 Health Connect `ExerciseSessionRecord` + `ExerciseType` enum 기반. 사용자 노출 동작은 동일.
