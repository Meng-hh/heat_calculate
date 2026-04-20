import { useState, useRef, useCallback, useEffect } from 'react';
import './UploadPage.css';

const ALLOWED_TYPES = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'];
const ALLOWED_EXTENSIONS = ['.jpg', '.jpeg', '.png', '.webp'];
const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
const COMPRESS_THRESHOLD = 2 * 1024 * 1024; // 2MB

function UploadPage({ onUpload, initialError = '' }) {
  const [selectedFile, setSelectedFile] = useState(null);
  const [preview, setPreview] = useState(null);
  const [note, setNote] = useState('');
  const [error, setError] = useState(initialError);
  const [isDragging, setIsDragging] = useState(false);
  const [mode, setMode] = useState('rough'); // 'rough' | 'refined'
  const fileInputRef = useRef(null);

  // 当 initialError 变化时更新错误信息
  useEffect(() => {
    if (initialError) {
      setError(initialError);
    }
  }, [initialError]);

  const validateFile = (file) => {
    if (!file) return '请选择图片';
    
    // 检查 MIME 类型或文件扩展名
    const fileType = file.type?.toLowerCase() || '';
    const fileName = file.name?.toLowerCase() || '';
    const hasValidType = ALLOWED_TYPES.includes(fileType);
    const hasValidExtension = ALLOWED_EXTENSIONS.some(ext => fileName.endsWith(ext));
    
    if (!hasValidType && !hasValidExtension) {
      return '仅支持 JPG、PNG、WEBP 格式的图片';
    }
    if (file.size > MAX_FILE_SIZE) {
      return '图片大小不能超过 10MB';
    }
    return '';
  };

  const compressImage = async (file) => {
    if (file.size <= COMPRESS_THRESHOLD) return file;

    return new Promise((resolve) => {
      const img = new Image();
      const canvas = document.createElement('canvas');
      const ctx = canvas.getContext('2d');

      img.onload = () => {
        let { width, height } = img;
        const maxDimension = 1920;
        
        if (width > maxDimension || height > maxDimension) {
          if (width > height) {
            height = (height / width) * maxDimension;
            width = maxDimension;
          } else {
            width = (width / height) * maxDimension;
            height = maxDimension;
          }
        }

        canvas.width = width;
        canvas.height = height;
        ctx.drawImage(img, 0, 0, width, height);

        canvas.toBlob(
          (blob) => {
            if (blob && blob.size < file.size) {
              const compressedFile = new File([blob], file.name, {
                type: file.type,
                lastModified: file.lastModified,
              });
              resolve(compressedFile);
            } else {
              resolve(file);
            }
          },
          file.type,
          0.85
        );
      };

      img.onerror = () => resolve(file);
      img.src = URL.createObjectURL(file);
    });
  };

  const handleFileSelect = async (file) => {
    setError('');
    const validationError = validateFile(file);
    if (validationError) {
      setError(validationError);
      return;
    }

    const compressedFile = await compressImage(file);
    setSelectedFile(compressedFile);
    setPreview(URL.createObjectURL(compressedFile));
  };

  const handleInputChange = (e) => {
    const file = e.target.files[0];
    if (file) handleFileSelect(file);
  };

  const handleDrop = useCallback((e) => {
    e.preventDefault();
    setIsDragging(false);
    const file = e.dataTransfer.files[0];
    if (file) handleFileSelect(file);
  }, []);

  const handleDragOver = useCallback((e) => {
    e.preventDefault();
    setIsDragging(true);
  }, []);

  const handleDragLeave = useCallback((e) => {
    e.preventDefault();
    setIsDragging(false);
  }, []);

  const handleSubmit = () => {
    if (!selectedFile) {
      setError('请先选择图片');
      return;
    }
    onUpload(selectedFile, note, mode);
  };

  const handleReset = () => {
    setSelectedFile(null);
    setPreview(null);
    setNote('');
    setError('');
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  return (
    <div className="upload-page">
      <div className="header">
        <h1 className="header-title">热量小助手 🔥</h1>
        <p className="header-subtitle">拍照识别食物热量，轻松管理饮食</p>
      </div>

      <div className="card">
        <div className="mode-toggle">
          <button
            className={`mode-btn ${mode === 'rough' ? 'active' : ''}`}
            onClick={() => setMode('rough')}
          >
            粗略模式
          </button>
          <button
            className={`mode-btn ${mode === 'refined' ? 'active' : ''}`}
            onClick={() => setMode('refined')}
          >
            精细模式
          </button>
        </div>

        {!preview ? (
          <div
            className={`upload-area ${isDragging ? 'dragover' : ''}`}
            onClick={() => fileInputRef.current?.click()}
            onDrop={handleDrop}
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
          >
            <div className="upload-icon">📷</div>
            <p className="upload-text">点击拍照或选择图片</p>
            <p className="upload-hint">支持 JPG、PNG、WEBP 格式，最大 10MB</p>
            <input
              ref={fileInputRef}
              type="file"
              accept="image/jpeg,image/jpg,image/png,image/webp"
              onChange={handleInputChange}
              style={{ display: 'none' }}
              capture="environment"
            />
          </div>
        ) : (
          <div className="preview-container">
            <img src={preview} alt="预览" className="preview-image" />
            <button className="btn btn-secondary preview-remove" onClick={handleReset}>
              重新选择
            </button>
          </div>
        )}

        {error && (
          <div className="error-message" style={{ marginTop: '16px' }}>
            <span>⚠️</span>
            {error}
          </div>
        )}

        <div className="note-section" style={{ marginTop: '24px' }}>
          <label className="note-label">备注（可选）</label>
          <input
            type="text"
            className="input"
            placeholder="例如：午餐、训练前..."
            value={note}
            onChange={(e) => setNote(e.target.value)}
            maxLength={100}
          />
        </div>

        <button
          className="btn btn-primary"
          style={{ width: '100%', marginTop: '24px' }}
          onClick={handleSubmit}
          disabled={!selectedFile}
        >
          开始分析
        </button>
      </div>

      <div className="tips">
        <p>💡 小贴士：使用标准餐具作为参照，识别更准确</p>
      </div>
    </div>
  );
}

export default UploadPage;
