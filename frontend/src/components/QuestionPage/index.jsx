import { useState } from 'react';
import './QuestionPage.css';

function QuestionPage({ question, onSubmit, partialResult }) {
  const [answer, setAnswer] = useState('');
  const [error, setError] = useState('');

  const handleSubmit = () => {
    if (!answer.trim()) {
      setError('请输入你的回答');
      return;
    }
    setError('');
    onSubmit(answer.trim());
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSubmit();
    }
  };

  return (
    <div className="question-page">
      <div className="header">
        <h1 className="header-title">精细分析 🔍</h1>
        <p className="header-subtitle">AI 需要更多信息来精确估算</p>
      </div>

      <div className="card question-card">
        <div className="question-icon">💬</div>
        <p className="question-text">{question}</p>

        <div className="answer-section">
          <textarea
            className="answer-input"
            placeholder="请输入你的回答..."
            value={answer}
            onChange={(e) => {
              setAnswer(e.target.value);
              if (error) setError('');
            }}
            onKeyDown={handleKeyDown}
            rows={3}
            maxLength={200}
          />
          {error && (
            <div className="error-message">
              <span>⚠️</span> {error}
            </div>
          )}
        </div>

        <button
          className="btn btn-primary"
          style={{ width: '100%', marginTop: '16px' }}
          onClick={handleSubmit}
          disabled={!answer.trim()}
        >
          确认
        </button>
      </div>

      {partialResult && partialResult.totalCalories && (
        <div className="card partial-result-card">
          <h3 className="partial-result-title">当前估算</h3>
          <div className="partial-result-range">
            {partialResult.totalCalories.low} - {partialResult.totalCalories.high} 千卡
          </div>
          <p className="partial-result-hint">回答问题后，估算将更精确</p>
        </div>
      )}
    </div>
  );
}

export default QuestionPage;
