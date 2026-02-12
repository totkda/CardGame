# CardGame (Android / Kotlin)

Up & Down／ババ抜き／大富豪が遊べる Android アプリです。  
初めての方でも 5〜10 分でビルド・プレイできます。

## 特徴
- **軽量・広告なし・直感的UI**
- **3つのゲームを搭載**
  - **Up & Down**：A最強→2最弱、**正解で×2／失敗で0&終了**（右カードは裏→押下後に表）  
  - **ババ抜き**：**JOKER 1 枚**入り、**初期ペア自動捨て**、**左隣からドロー**、最後の1人が敗者  
  - **大富豪**：**JOKERあり**＋**役（シングル／ペア・スリー・フォー／階段）**＋**革命・縛り・8切り**対応  
- UIは**縦長**想定、カードは**赤黒配色**・**コーナー表記**、場は**役ごとにレイアウト**（扇形／横並び）  
（要件・設計の詳細は `SPEC.md` 参照）


## 動作環境
- Android Studio Koala 以降
- **Android 7.0 (API 24)** 以上の端末／エミュレータ  
- Kotlin / Compose / AGP の整合：**Kotlin 1.9.25 + Compose Compiler 1.5.15 + AGP 8.6.1** を推奨  
  - `material-icons-extended`（戻るアイコン）, `compose-animation`（AnimatedContent）依存を導入済み  
  - Experimental API（`AnimatedContent`）は Opt-in しています（`@OptIn(ExperimentalAnimationApi::class)`）
  

## セットアップ（5分）
1. リポジトリをクローン  
2. Android Studio で開く → **Gradle Sync**  
3. ビルドが通らない場合は下の「ビルドで詰まりやすいポイント」を参照  
4. 右上の ▶ で実行（エミュレータ or 実機）

**最初の起動画面**は「**ゲーム選択**」。  
- **Up & Down** を押すとハイ＆ロー画面へ（右カードは裏、**Up/Down**押下で表）  
- **ババ抜き** を押すと Old Maid（**左隣からタップで引く**）  
- **大富豪** を押すと役あり版が開始（**複数選択→出す**、**パス**、場は**扇／横並び**表示）

## ビルドで詰まりやすいポイント
- **Compose BOM の重複宣言**：`val composeBom = platform(...)` は **dependencies 内で1回のみ**にしてください。  
- **Icons 未解決**：`implementation("androidx.compose.material:material-icons-extended")` を追加し、`import androidx.compose.material.icons.*` を入れる。  
- **Experimental animation**：`compose-animation` を依存に加え、`@OptIn(ExperimentalAnimationApi::class)` を付与。  
（これらは開発中に実際に発生し、解決済みの内容です） 

## ディレクトリ（主要）
app/
└─ src/main/java/com/example/app/cardgame/
├─ AppRoot.kt                # ルート（ゲーム選択 ↔ 各ゲーム）
├─ Card.kt                   # Suit/JOKER & Card（共通）
├─ HighLowScreen.kt          # Up & Down 画面
├─ HighLowViewModel.kt       # Up & Down ロジック（×2/0&終了、A最強…2最弱）
├─ OldMaidScreen.kt          # ババ抜きUI（左隣の裏カードタップ）
├─ OldMaidViewModel.kt       # ババ抜きロジック（初期ペア捨て、左隣ドロー）
├─ DaifugoModels.kt          # 大富豪 役・比較・8切り/縛り/革命ユーティリティ
├─ DaifugoViewModel.kt       # 大富豪 ロジック本体
└─ DaifugoScreen.kt          # 大富豪 UI（場=FieldArea のみ）

（画面・遷移や UI 方針は、共有ドキュメントの基本設計に沿っています）


## プレイガイド（簡易）
### Up & Down
- 左=前のカード（表）、右=新規カード（裏）  
- **Up/Down を押す** → 右が**表** → **A>…>2** で判定  
- **正解ならスコア×2**、**外れたら0で終了**（リスタート可）

### ババ抜き
- **あなたの番**：中央の**左隣の裏カード列**をタップして**1枚引く** → 直後に**ペア自動捨て**  
- 手札0で**着順記録**、**最後に残った人（JOKER保持）**が敗者

### 大富豪
- 手札から**1枚以上を選択** → **出す**  
- 役：**シングル／グループ（同ランク 2〜4、JOKERはペアのみワイルド）／階段（同スート連番3+）**  
- **革命**：4枚出しで反転（JOKERは常に最強）  
- **縛り**：同スート2連続で縛り発動、**流し**で解除  
- **8切り**：8を含む役で**即流し**、同プレイヤーが続行リード

## テスト
`app/src/test/...` にユニットテストを配置。優先度は以下。  
- デッキの一意性／枚数（52/53）  
- 役判定（`classifySelection`）と比較（`beats`）  
- Up & Down のスコア遷移  
- ババ抜きの初期ペア削除と終了判定  
（要件・テスト方針はドキュメント参照）


## 参考・根拠
- チームの要件定義／基本設計／進捗メモに準拠（人数、JOKER有無、左隣ドロー、画面遷移、得点仕様、カード強弱など）。
