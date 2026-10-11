# 🛠 Marmara Arşiv README Oluşturucu

Marmara Üniversitesi bölüm arşivlerinin (ilki [Bilgisayar Mühendisliği](https://github.com/MarmaraArsiv/tek_bilgisayar))
bütün README'lerini (ana sayfa, dönemler, seçmeli havuzları ve her dersin kendi sayfası) JSON verilerinden üreten
masaüstü aracı. Ders, hoca ya da dönem bilgisi değiştiğinde README'leri elle düzenlemek yerine veriyi arayüzden girip
aracı çalıştırmak yeterli.

Araç bütün bölümler için ortaktır: bölüme özel her şey (başlık, dönemler, seçmeli havuzları, müfredat seçenekleri,
Google Form linkleri) arşivin kendi `.github/json_dosyalari/` klasöründedir. Yeni bir bölümün arşivi araçtan
tek pencereyle kurulur.

## Kurulum (bir kez)

Gerekenler: **JDK 21** (Java), **Git** ve bu araçla düzenleyeceğin **arşiv reposu**. Maven ya da başka bir şey
kurmaya gerek yok; araç ilk açılışta gerekenleri kendisi indirir.

### 🐧 Ubuntu / Linux

```bash
sudo apt install openjdk-21-jdk git

# Araç ve arşiv aynı klasörde, yan yana dursun:
mkdir -p ~/marmara && cd ~/marmara
git clone git@github.com:MarmaraArsiv/Marmara_Arsiv_Readme_Olusturucu.git
git clone git@github.com:MarmaraArsiv/tek_bilgisayar.git
```

`git@github.com:` adresleri GitHub'a SSH anahtarıyla bağlanır. Anahtarın yoksa bir kez oluşturup GitHub'a ekle:

```bash
ssh-keygen -t ed25519            # sorulara Enter ile geç
cat ~/.ssh/id_ed25519.pub        # çıkan satırı GitHub → Settings → SSH and GPG keys → New SSH key'e yapıştır
```

### 🪟 Windows

1. **Java:** [adoptium.net](https://adoptium.net) → **Temurin 21 (LTS)** → Windows, x64, `.msi` indir ve kur.
   Kurulumda **"Set JAVA_HOME variable"** seçeneğini aç (varsayılan olarak kapalıdır).
2. **Git:** [git-scm.com/download/win](https://git-scm.com/download/win) → indir, varsayılan ayarlarla kur.
   İlk pushta tarayıcıda GitHub girişi açılır, şifre ya da token yazman gerekmez.
3. **Repoları indir:** Başlat menüsünden **Git Bash**'i aç ve şunları yaz (araç ve arşiv yan yana dursun):
   ```bash
   mkdir -p ~/marmara && cd ~/marmara
   git clone https://github.com/MarmaraArsiv/Marmara_Arsiv_Readme_Olusturucu.git
   git clone https://github.com/MarmaraArsiv/tek_bilgisayar.git
   ```
   Klasörler `C:\Users\<kullanıcı adın>\marmara` altına iner.

> ZIP olarak indirmek yerine `git clone` kullan: araç değişiklikleri GitHub'a gönderirken arşivin git ile
> indirilmiş olmasına ihtiyaç duyar. Arşive doğrudan pushlamak için
> [MarmaraArsiv](https://github.com/MarmaraArsiv)'de yazma yetkin olmalı.

## Aracı açma

| | 🐧 Ubuntu / Linux | 🪟 Windows |
|---|---|---|
| Arayüz | Terminalde: `~/marmara/Marmara_Arsiv_Readme_Olusturucu/arayuz.sh` | Klasörde `arayuz.bat`'a **çift tıkla** |
| Aracı güncelleme | Araç klasöründe `git pull` | Git Bash'te araç klasöründe `git pull` |

- İlk açılış internetten Maven'i ve kütüphaneleri indirdiği için birkaç dakika sürer; sonrakiler hızlıdır.
  Araç güncellendiyse bir sonraki açılışta kendiliğinden yeniden derlenir.
- Windows'ta arkada siyah bir komut penceresi açık kalır. Onu kapatırsan araç da kapanır.
- Windows "Windows kişisel bilgisayarınızı korudu" derse **Ek bilgi → Yine de çalıştır**.
- Java bulunamazsa pencere ne yapman gerektiğini yazar (Windows'ta yukarıdaki 1. adım).
- Araç, yanındaki klasörlerde bulduğu arşivi açar. Bulamazsa klasörü sorar; son açılan arşivleri hatırlar.

## Kullanım

Önerilen sıra:

1. **Git İşlemleri › ⬇️ GitHub'dan Çek** — GitHub'daki son hâli al (botlar README'leri sık günceller).
2. **Ders / Hoca / Dönem / Giriş** pencerelerinden düzenle. Değişiklikler JSON dosyalarına kaydedilir.
3. **Git İşlemleri › 📝 Readme Güncelle** — README'leri JSON'dan üretir, yalnızca değişenleri yazar.
4. **⬆️ Değişiklikleri GitHub'a Pushla** — değişen dosyaları ve farklarını gör, commit mesajını yaz.
   Araç önce GitHub'daki bot commit'lerini alır, seninkini üstüne koyup gönderir. Bot aynı satırları
   değiştirmişse senin sürümün geçerli olur; botlar yıldız/yorum bloklarını birkaç dakika içinde yeniden
   yazar. Push yarıda kalırsa (ör. internet yoksa) commit kaybolmaz, tekrar Pushla'ya basman yeterli.

Başka bölümün arşivine ana menüdeki **📂 Başka Bölümün Arşivini Aç** ile geçilir.

## Yeni bölüm arşivi

Ana menüde **🆕 Yeni Bölüm Arşivi Oluştur**: bölüm ve fakülte adı, GitHub reposu (ör.
`https://github.com/MarmaraArsiv/tek_elektrik`), sınıf sayısı, seçmeli havuzları ve müfredat seçenekleri sorulur.
Araç şunları kurar:

- her sınıf için Güz/Bahar dönem klasörleri (`1-1` … `4-2`) ve havuz klasörleri, README'leriyle;
- JSON verileri (Bilgisayar arşivindeki metinlerden, bölüm adıyla);
- GitHub betikleri ve iş akışları (yıldızlar, onaylı yorumlar, doluluk tablosu, katkıda bulunanlar);
- Google Form ve Telegram onay botu betikleri (`.github/google_form/`), `CONTRIBUTING.md`, `STANDARTLAR.md`, `LICENSE`.

Sonraki adımlar (GitHub reposu, Actions ayarları, formların kurulması) arşivdeki
`.github/google_form/KURULUM.md`'de. Formlar kurulana kadar README'lerde form linkleri yerine "Henüz yorum yok."
yazar; `form_olustur.gs` kurulumun sonunda araca yapıştırılacak form ayarlarını kendisi yazdırır
(**Konfigürasyon Düzenle › Form ayarlarını yapıştır**).

### Ortak altyapı

GitHub betikleri, iş akışları ve form betikleri bütün bölüm arşivlerinde aynıdır ve tek kaynağı bu repodaki
[`src/main/resources/sablon/`](src/main/resources/sablon) klasörüdür (`dosyalar.txt` hangi dosyanın nereye gittiğini
söyler). Bir betik iyileştirilince:

1. değişikliği `sablon/` altında yap;
2. her arşivde **Git İşlemleri › 🧰 Altyapıyı Güncelle** (ya da `--altyapi-guncelle`) ve pushla.

Testler Bilgisayar arşivindeki betiklerin şablonla aynı olduğunu da denetler.

## Komut satırı (isteğe bağlı)

Arayüzün yaptıklarının çoğu komut satırından da yapılabilir. Linux'ta `readme_olustur.sh`, Windows'ta
`readme_olustur.bat` (Komut İstemi'nde, araç klasöründe):

```bash
./readme_olustur.sh                             # README'leri JSON'dan üretir (yandaki tek arşiv)
./readme_olustur.sh <arşiv>                     # belirli bir arşiv
./readme_olustur.sh <arşiv> --kontrol           # hiçbir şey yazmaz; README'ler JSON ile uyumlu mu bakar
./readme_olustur.sh <arşiv> --altyapi-kontrol   # GitHub betikleri aracın şablonuyla aynı mı
./readme_olustur.sh <arşiv> --altyapi-guncelle  # GitHub betiklerini şablonla eşitler
./readme_olustur.sh --yeni-arsiv ../tek_elektrik --bolum "Elektrik-Elektronik Mühendisliği" \
    --fakulte "Teknoloji Fakültesi" --github https://github.com/MarmaraArsiv/tek_elektrik \
    --havuz "Mesleki Seçmeli" --havuz "Üniversite Seçmeli" --drive "Elektrik-Teknoloji"
./build.sh                                      # yalnızca derler: target/marmara-readme-araci.jar
```

Windows'ta aynı komutlar `readme_olustur.bat ..\tek_bilgisayar --kontrol` biçimindedir (`--yeni-arsiv` için
arayüzdeki **🆕 Yeni Bölüm Arşivi Oluştur** düğmesini kullan).

`--kontrol` ve `--altyapi-kontrol` fark varsa listeleyip 1 koduyla çıkar; JSON'da hata varsa 2 koduyla.
`--yeni-arsiv`'de `--sinif` (varsayılan 4) ve tekrarlanabilen `--mufredat` da verilebilir; `--mufredat`
verilirse eski müfredat notu kapalı olur.

## Veriler

Arşivde `.github/json_dosyalari/` altında:

| Dosya | İçerik |
|---|---|
| `dersler.json` | Her ders: klasörü (ör. `2-2/Elektronik Devrelere Giriş`), tipi, müfredatı ve notu, yıl yıl dersi yürüten hocalar, derse dair öneriler. Listedeki sıra README'deki sıradır. |
| `hocalar.json` | Her hoca: kısaltma, ana README'deki grubu, ofis, AVESİS linki, cinsiyet (kart emojisi), verdiği dersler. Grupların sırası ve açıklamaları. |
| `donemler.json` | Dönemler (başlık, genel tavsiyeler) ve seçmeli havuzları (başlık, açıklama). |
| `giris.json` | Ana sayfanın başı: başlık, açıklama, geri bildirim paragrafı, Katkıda Bulunun bölümü, sayaç. |
| `repo_kullanimi.json` | Repo Kullanımı ve Kavramlar bölümleri. |
| `katkida_bulunanlar.json` | Katkıda bulunanlar (GitHub'daki betik pull request'lerden günceller). |
| `konfigurasyon.json` | Repo adresi, Google Form linkleri ve soru kimlikleri, müfredat seçenekleri ve eski müfredat notu kuralı, README'lerin yeri (`dokumanlar_repo_yolu`). |

## Nasıl çalışır

- Her README JSON'dan baştan üretilir. Veri değişmediyse çıktı mevcut README ile **birebir aynıdır**; bunu
  gerçek arşive karşı çalışan test doğrular.
- GitHub'daki betiklerin doldurduğu bloklar mevcut README'den aynen korunur: ⭐ yıldızlar, 💬 onaylı yorumlar,
  hoca yorumları ve 📊 doluluk tablosu. Yeni ders ya da hoca için bu blokların boş hâli yazılır; GitHub ilk
  push'ta doldurur.
- Kurallar:
  - Hocalar grup içinde, kısaltmalar ise listede Türkçe alfabeye göre dizilir.
  - Hoca kartındaki dersler en son verildiği yıla göre sıralanır.
  - Ana sayfadaki ders kartında yalnızca en güncel yılın hocaları görünür.
  - Eski müfredattaki bir ders geçiş yılı ve sonrasında açıldıysa yılın yanına açıklama eklenir
    (Bilgisayar'da: "2026 Öncesi" dersleri 2026-2027'den itibaren "bu yıl yeni girenler bu dersi alamaz").
    Önek, yıl ve metin `konfigurasyon.json`'daki `mufredat` alanındadır; önek boşsa kural kapalıdır.
- Üretimden önce JSON doğrulanır: boş ya da tekrarlanan klasör, yanlış yıl biçimi, çakışan kısaltma veya
  tanımsız grup gibi hatalarda **hiçbir dosyaya dokunulmaz**, hataların listesi gösterilir.
- JSON dosyaları Python'un `json.dumps(indent=4)` biçiminde yazılır ve araç tanımadığı alanları silmez;
  böylece arayüzden kaydetmek GitHub betikleriyle çakışmaz.

## Testler

```bash
./mvnw test -Darsiv.json=../Marmara_Bilgisayar_Muhendisligi_Arsiv
```

Denetlenenler:

- Arşivdeki README'ler JSON'dan birebir üretiliyor; JSON'lar okunup yazılınca değişmiyor.
- Yeni bir ders eklenince ders sayfası, havuz listesi, ana sayfa kartı ve hoca kartı doğru oluşuyor.
- Bütün pencereler ve düzenleme diyalogları ekransız (Monocle) açılıp hatasız kapanıyor.
- Arayüz bir kullanıcı gibi kullanılıyor (alan doldurma, düğmeye basma, onay kutuları):
  - ders ekleme (yıl, listeden hoca, not, listede olmayan isim), düzenleme, silme ve Ctrl+Z ile geri alma;
  - hoca ekleme (kısaltma ve grup önerisi) ve terfi: ünvan değişince grup, derslerdeki ad ve onaylı
    yorumlar yeni ada taşınıyor;
  - Readme Güncelle yalnızca değişen dosyaları yazıyor, JSON hatalıysa hiçbir dosyaya dokunmuyor;
  - push: yerel bir test reposunda boşluklu/Türkçe adlı dosyalar seçilip commit'leniyor, botun arada
    attığı commit'in üstüne gönderiliyor (bot aynı satırı değiştirdiyse kullanıcının sürümü geçerli),
    işaretlenmeyen değişiklik yerelde kalıyor; yarıda kalmış bir push sonradan tamamlanabiliyor.
- Yeni bölüm arşivi şablondan eksiksiz kuruluyor: yer tutucuların hepsi doluyor, Bilgisayar'a özel metin kalmıyor,
  kendisiyle uyumlu; GitHub betikleri yeni arşivde çalışıyor (python3 varsa); formlar kurulmadan README'ler
  linksiz, kurulunca linkli üretiliyor; müfredat notu bölüme göre ayarlanıyor.
- Sihirbaz ve Konfigürasyon penceresi (form ayarlarını yapıştırma) bir kullanıcı gibi kullanılıyor.
- Bilgisayar arşivindeki GitHub betikleri aracın şablonuyla aynı.
- Link ve sıralama yardımcıları doğru çalışıyor.

`-Darsiv.json` verilmezse arşive bağlı testler atlanır. Testler arşive hiçbir şey yazmaz; hepsi geçici
kopyalarla çalışır, push testi de GitHub'a değil geçici bir yerel repoya gönderir.

## Sorun giderme

| Belirti | Çözüm |
|---|---|
| "JDK 21 bulunamadı" | JDK 21'i yukarıdaki gibi kur. Başka bir klasöre kurduysan `JAVA_HOME`'u o klasöre ayarla: Linux'ta `export JAVA_HOME=/jdk/klasörü`; Windows'ta **Başlat → "ortam değişkenleri" → Ortam Değişkenleri → Yeni** (ad `JAVA_HOME`, değer JDK klasörü, ör. `C:\Program Files\Eclipse Adoptium\jdk-21…`). |
| Derleme internet hatasıyla duruyor | İlk açılışta internet gerekir; bağlantıyı kontrol edip tekrar aç. |
| Push "Permission denied" / "403" veriyor | GitHub'da MarmaraArsiv'e yazma yetkin yok ya da giriş yapılmadı. Linux'ta SSH anahtarını [GitHub'a ekle](https://github.com/settings/keys) (Kurulum › Ubuntu); Windows'ta açılan tarayıcı penceresinden GitHub'a giriş yap. |
| Araç arşivi bulamıyor | Arşiv klasörünü sorduğunda seç ya da aracı arşivle aynı klasöre koy (`marmara/` altında yan yana). |
