# Ballet Log 1.6 — Play Console 업로드 자료

versionCode 4 / versionName 1.6
AAB: `app/build/outputs/bundle/release/app-release.aab`

---

## Release Notes

### 한국어

```
새 기능
• 포토 로그: 사진과 캡션을 자유롭게 기록하는 새 탭
• 메모 개선: 사진 첨부, 클래스 연결, YouTube/Instagram 링크 카드
• 백업·복원: 설정에서 전체 데이터를 한 파일로 내보내고 가져오기
• Apple Health(헬스 커넥트) 워크아웃 가져오기 안정화
• 일본어 전체 번역
```

### English

```
New
• Photo Log: a dedicated tab for moments with captions, filters and workout overlays
• Notes: photo attachments, link to a class, rich link cards (YouTube/Instagram)
• Backup & Restore in Settings — export your whole library as one file
• Health Connect workout fetch hardened
• Full Japanese translation
```

### 日本語

```
新機能
• フォトログ:キャプションやワークアウト情報付きの瞬間を残す専用タブ
• メモ:写真の添付、クラスへのリンク、YouTube/Instagram リンクカード
• 設定からデータ全体を1ファイルに書き出し・取り込み
• Health Connect のワークアウト取得を安定化
• 日本語の全画面翻訳
```

---

## Health Connect 권한 사용 설명

### `android.permission.health.READ_TOTAL_CALORIES_BURNED`

#### 한국어

```
이 앱은 사용자의 발레/운동 클래스 기록에 자동으로 칼로리 정보를 첨부하기
위해 READ_TOTAL_CALORIES_BURNED 권한을 사용합니다.

구체적인 사용 흐름:
1) 사용자가 클래스 또는 포토 로그를 작성하면서 "발레 워크아웃 불러오기" 또는
   "Workout fetch" 버튼을 누릅니다.
2) 앱은 해당 날짜에 기록된 운동 세션(ExerciseSession)을 조회하고, 그 세션의
   시간 범위 안에서 활성 칼로리(ActiveCaloriesBurned)를 우선 읽어옵니다.
3) 일부 기기(예: 갤럭시워치)는 ActiveCaloriesBurned 대신 총 소모 칼로리
   (TotalCaloriesBurned)만 기록하기 때문에, ActiveCalories 값이 0이거나
   존재하지 않을 때 동일한 시간 범위의 TotalCaloriesBurned로 대체합니다.
4) 가져온 숫자는 사용자가 직접 작성 중인 단일 클래스 로그 또는 포토 로그에만
   기록되며, 사용자 기기 안의 로컬 데이터베이스에 저장됩니다.

데이터는 외부 서버로 전송되거나 제3자에게 공유되지 않으며, 광고·분석에도
사용되지 않습니다. 권한은 Health Connect 의 표준 권한 다이얼로그를 통해
사용자가 직접 허용/철회할 수 있습니다.
```

#### English

```
This app uses READ_TOTAL_CALORIES_BURNED to automatically attach a calorie
estimate to a ballet/exercise class log the user is creating.

Flow:
1) The user creates a class log or photo log and taps "Find Ballet Workout".
2) The app queries Health Connect for an ExerciseSessionRecord on that date
   and first reads ActiveCaloriesBurnedRecord within that session's time
   range.
3) Some devices (e.g. Galaxy Watch) only log TotalCaloriesBurnedRecord
   instead of ActiveCaloriesBurned. When ActiveCalories returns 0 or no
   records, the app falls back to TotalCaloriesBurnedRecord for the same
   time range.
4) The resulting number is written only to the single class/photo log the
   user is editing, and is stored locally in the on-device database.

The data is never transmitted to a server, never shared with third parties,
and is not used for advertising or analytics. The user can grant or revoke
the permission at any time through the Health Connect permissions UI.
```

#### Play Console 부가 항목

- **Data type**: Calories burned (Total)
- **Collected / Shared**: Collected — 기기 내부에만 저장, 외부 공유 없음
- **Optional / Required**: Optional — 권한이 없어도 앱의 다른 기능은 모두 동작
- **Why optional**: 사용자가 워크아웃 자동 가져오기를 사용하지 않으면 모든
  화면이 권한 없이 정상 작동. 사용자는 Manual entry 로 직접 칼로리를
  입력할 수 있음.

---

### 기존 권한도 동일 양식으로 (참고)

같은 흐름이라 본문은 거의 동일. Play Console이 이미 이전 release 에서
받은 설명을 가지고 있다면 그대로 두고 TOTAL_CALORIES 만 새로 작성하면
충분합니다.

#### `android.permission.health.READ_EXERCISE`

```
사용자가 작성 중인 클래스/포토 로그의 날짜에 기록된 운동 세션
(ExerciseSessionRecord)을 찾아서 운동 시간(분)을 자동 입력하기 위해
사용합니다. 가져온 데이터는 해당 로그에만 저장되며 외부로 전송되지 않습니다.
```

#### `android.permission.health.READ_ACTIVE_CALORIES_BURNED`

```
사용자가 작성 중인 클래스/포토 로그의 운동 세션 시간 범위에서 활성
소모 칼로리(ActiveCaloriesBurnedRecord)를 읽어 자동 입력하기 위해 사용합니다.
이 값을 받지 못한 경우 TotalCaloriesBurned 로 대체합니다.
```

#### `android.permission.health.READ_HEART_RATE`

```
사용자가 작성 중인 클래스/포토 로그의 운동 세션 시간 범위에서
평균/최대 심박수(HeartRateRecord)를 읽어 자동 입력하기 위해 사용합니다.
가져온 값은 해당 로그에만 저장되고 외부로 전송되지 않습니다.
```

---

## 업로드 절차 요약

1. Play Console → Closed testing (비공개 테스트) → **Create new release**
2. **App bundle** 에 `app-release.aab` 업로드
3. **Release notes** 에 위 한/영/일 텍스트 붙여넣기
4. (필요 시) **App content → Health Connect 권한** 항목 업데이트:
   `READ_TOTAL_CALORIES_BURNED` 추가 + 위 설명 입력
5. **Save → Review release → Roll out**

---

## 정식 출시(Production) 시점에 결정할 것

비공개 테스트 14일 카운트 끝나고 production 트랙으로 promote 하기 직전에
체크할 항목:

### 1. versionCode 올리기

비공개 테스트에 올라간 마지막 빌드가 versionCode 4. production 새 release
는 **versionCode 5** 이상으로 빌드해야 함.
`app/build.gradle.kts` → `versionCode = 5`, `versionName = "1.6.1"` 등.

### 2. 백업 배너 노출 정책

`BackupBannerPreferences` 가 만든 인앱 배너는 "플레이스토어 정식 출시 전에
백업해두세요" 안내. 정식 출시 후에는 의미가 없는 메시지.

선택지:
- **A. 그대로 두기** — 새 사용자도 한 번씩 보고 X 로 닫음. 약간 어색하지만
  코드 변경 없음. 첫 export 후엔 자동으로 사라짐.
- **B. 비활성화** — `BackupBannerPreferences.shouldShow()` 가 항상 false
  를 반환하도록 한 줄 수정. 또는 호출 위치(PhotoLogScreen)에서
  `bannerVisible = false` 로 바로 강제.
- **C. 코드 삭제** — `BackupBanner.kt` + `BackupBannerPreferences.kt` +
  PhotoLogScreen 의 호출부 모두 제거. 가장 깔끔.

추천: **B 또는 C**. production 빌드 직전에 결정.

### 3. release notes 톤 조정

비공개 테스트용 release notes 에는 백업 권유가 들어가 있는데, production
용은 "신기능 안내" 만 남기는 게 자연스러움. 위 한/영/일 노트의 "🩰
사용자분들께" 블록은 제거.

### 4. 다른 스토어 동기화

원스토어 1.6 가 4명 사용자에게 배포된 상태라면, production 출시 후 그
사용자들에게도 마이그레이션 안내가 필요할 수 있음 (현재 백업 배너로 이미
처리되어 있긴 함). 별도 푸시/메시지가 필요한지는 그 때 판단.

### 5. AAB 빌드 명령

```bash
# versionCode/Name 올린 뒤
./gradlew bundleRelease
# → app/build/outputs/bundle/release/app-release.aab
```
