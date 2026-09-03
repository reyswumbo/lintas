import asyncio
import os
import secrets
import string
import time
from contextlib import asynccontextmanager
from datetime import datetime, timedelta, timezone
from pathlib import Path

import aiofiles
from fastapi import BackgroundTasks, FastAPI, File, HTTPException, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse, JSONResponse
from pydantic import BaseModel
import sqlite3

ALPHABET = string.ascii_letters + string.digits

DATA_DIR = Path(__file__).parent / "data"
UPLOADS_DIR = DATA_DIR / "uploads"
DB_PATH = DATA_DIR / "transfers.db"

UPLOAD_EXPIRY_HOURS = 24
MAX_UPLOAD_SIZE = 500 * 1024 * 1024  # 500MB
CLEANUP_INTERVAL_SECONDS = 60


def get_db() -> sqlite3.Connection:
    conn = sqlite3.connect(str(DB_PATH))
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA journal_mode=WAL")
    return conn


def init_db():
    DATA_DIR.mkdir(parents=True, exist_ok=True)
    UPLOADS_DIR.mkdir(parents=True, exist_ok=True)
    conn = get_db()
    conn.execute("""
        CREATE TABLE IF NOT EXISTS transfers (
            code TEXT PRIMARY KEY,
            filename TEXT NOT NULL,
            filepath TEXT NOT NULL,
            size INTEGER NOT NULL,
            content_type TEXT NOT NULL,
            created_at TEXT NOT NULL,
            expires_at TEXT NOT NULL,
            status TEXT NOT NULL DEFAULT 'active'
        )
    """)
    conn.commit()
    conn.close()


def generate_code() -> str:
    part1 = "".join(secrets.choice(ALPHABET) for _ in range(5))
    part2 = "".join(secrets.choice(ALPHABET) for _ in range(5))
    return f"{part1}-{part2}"


def is_expired(expires_at: str) -> bool:
    exp = datetime.fromisoformat(expires_at)
    return datetime.now(timezone.utc) > exp


def cleanup_expired_files():
    conn = get_db()
    rows = conn.execute(
        "SELECT code, filepath FROM transfers WHERE status = 'active'"
    ).fetchall()
    removed = 0
    for row in rows:
        if is_expired(row["expires_at"]):
            filepath = Path(row["filepath"])
            if filepath.exists():
                filepath.unlink()
            conn.execute(
                "UPDATE transfers SET status = 'expired' WHERE code = ?",
                (row["code"],),
            )
            removed += 1
    conn.commit()
    conn.close()
    if removed:
        print(f"Cleanup: removed {removed} expired file(s)")


async def periodic_cleanup():
    while True:
        await asyncio.sleep(CLEANUP_INTERVAL_SECONDS)
        cleanup_expired_files()


@asynccontextmanager
async def lifespan(app: FastAPI):
    init_db()
    task = asyncio.create_task(periodic_cleanup())
    yield
    task.cancel()


app = FastAPI(title="Lintas", version="1.0.0", lifespan=lifespan)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


class TransferInfo(BaseModel):
    code: str
    filename: str
    size: int
    type: str
    created_at: str
    expires_at: str


class TransferDetail(BaseModel):
    code: str
    filename: str
    size: int
    type: str
    status: str
    created_at: str
    expires_at: str


@app.post("/api/upload", response_model=TransferInfo)
async def upload_file(file: UploadFile = File(...)):
    contents = await file.read()
    if len(contents) > MAX_UPLOAD_SIZE:
        raise HTTPException(
            status_code=413,
            detail=f"File too large. Maximum size is {MAX_UPLOAD_SIZE // (1024*1024)}MB",
        )

    code = generate_code()
    now = datetime.now(timezone.utc)
    expires_at = now + timedelta(hours=UPLOAD_EXPIRY_HOURS)
    ext = Path(file.filename or "upload").suffix
    filename_stored = f"{code}{ext}"
    filepath = UPLOADS_DIR / filename_stored

    async with aiofiles.open(filepath, "wb") as f:
        await f.write(contents)

    conn = get_db()
    conn.execute(
        """INSERT INTO transfers (code, filename, filepath, size, content_type, created_at, expires_at, status)
           VALUES (?, ?, ?, ?, ?, ?, ?, 'active')""",
        (
            code,
            file.filename or "upload",
            str(filepath),
            len(contents),
            file.content_type or "application/octet-stream",
            now.isoformat(),
            expires_at.isoformat(),
        ),
    )
    conn.commit()
    conn.close()

    return TransferInfo(
        code=code,
        filename=file.filename or "upload",
        size=len(contents),
        type=file.content_type or "application/octet-stream",
        created_at=now.isoformat(),
        expires_at=expires_at.isoformat(),
    )


@app.get("/api/transfer/{code}", response_model=TransferDetail)
async def get_transfer(code: str):
    conn = get_db()
    row = conn.execute(
        "SELECT * FROM transfers WHERE code = ? COLLATE BINARY", (code,)
    ).fetchone()
    conn.close()

    if not row:
        raise HTTPException(status_code=404, detail="Transfer not found")

    if row["status"] != "active":
        raise HTTPException(status_code=410, detail="Transfer has expired")

    if is_expired(row["expires_at"]):
        conn = get_db()
        conn.execute(
            "UPDATE transfers SET status = 'expired' WHERE code = ?", (code,)
        )
        conn.commit()
        conn.close()
        raise HTTPException(status_code=410, detail="Transfer has expired")

    return TransferDetail(
        code=row["code"],
        filename=row["filename"],
        size=row["size"],
        type=row["content_type"],
        status=row["status"],
        created_at=row["created_at"],
        expires_at=row["expires_at"],
    )


@app.get("/api/download/{code}")
async def download_file(code: str):
    conn = get_db()
    row = conn.execute(
        "SELECT * FROM transfers WHERE code = ? COLLATE BINARY", (code,)
    ).fetchone()
    conn.close()

    if not row:
        raise HTTPException(status_code=404, detail="Transfer not found")

    if row["status"] != "active":
        raise HTTPException(status_code=410, detail="Transfer has expired")

    if is_expired(row["expires_at"]):
        conn = get_db()
        conn.execute(
            "UPDATE transfers SET status = 'expired' WHERE code = ?", (code,)
        )
        conn.commit()
        conn.close()
        raise HTTPException(status_code=410, detail="Transfer has expired")

    filepath = Path(row["filepath"])
    if not filepath.exists():
        raise HTTPException(status_code=404, detail="File not found on disk")

    return FileResponse(
        path=str(filepath),
        filename=row["filename"],
        media_type=row["content_type"],
    )


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
