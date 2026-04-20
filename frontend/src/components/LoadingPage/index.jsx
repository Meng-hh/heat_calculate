import './LoadingPage.css';

function LoadingPage() {
  return (
    <div className="loading-page">
      <div className="loading-content">
        <div className="loading-animation">
          <span className="loading-food">🍎</span>
          <span className="loading-food">🥗</span>
          <span className="loading-food">🍗</span>
        </div>
        <h2 className="loading-title">正在识别食物中...</h2>
        <p className="loading-subtitle">AI 正在分析图片，请稍候</p>
        <div className="loading-progress">
          <div className="loading-progress-bar"></div>
        </div>
      </div>
    </div>
  );
}

export default LoadingPage;
