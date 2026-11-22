import gallery_dl
import json

CHROME_UA = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
    "AppleWebKit/537.36 (KHTML, like Gecko) "
    "Chrome/131.0.0.0 Safari/537.36"
)

def apply_impersonate():
    gallery_dl.config.set(("extractor",), "browser", "chrome")
    gallery_dl.config.set(("extractor",), "user-agent", CHROME_UA)

def extract_info(url, limit=30):
    apply_impersonate()
    results = []
    class LimitReached(Exception):
        pass

    class InfoJob(gallery_dl.job.DataJob):
        def handle_url(self, url, kwdict):
            results.append({
                "url": url,
                "title": str(kwdict.get("title") or kwdict.get("description") or kwdict.get("filename") or ""),
                "uploader": str(kwdict.get("author", {}).get("name") if isinstance(kwdict.get("author"), dict) else (kwdict.get("user") or kwdict.get("uploader") or "")),
                "ext": str(kwdict.get("extension") or "jpg"),
                "id": str(kwdict.get("id") or kwdict.get("post_id") or len(results))
            })
            if len(results) >= limit:
                raise LimitReached()
        def handle_directory(self, kwdict):
            pass

    try:
        job = InfoJob(url)
        job.run()
    except LimitReached:
        pass
    return json.dumps(results)

def download_gallery(url, output_dir):
    apply_impersonate()
    gallery_dl.config.set(("extractor",), "base-directory", output_dir)
    gallery_dl.config.set(("extractor",), "directory", [])
    job = gallery_dl.job.DownloadJob(url)
    return job.run()
