import { useState, useCallback } from 'react';
import UploadPage from './components/UploadPage';
import LoadingPage from './components/LoadingPage';
import ResultPage from './components/ResultPage';
import QuestionPage from './components/QuestionPage';
import { analyzeFood, startRefinedAnalysis, continueRefinedAnalysis } from './api';
import './styles/global.css';
import './App.css';

// App states
const STATES = {
  UPLOAD: 'upload',
  LOADING: 'loading',
  RESULT: 'result',
  QUESTIONING: 'questioning',
};

function App() {
  const [currentState, setCurrentState] = useState(STATES.UPLOAD);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  const [sessionId, setSessionId] = useState(null);
  const [currentQuestion, setCurrentQuestion] = useState(null);
  const [partialResult, setPartialResult] = useState(null);

  const handleUpload = useCallback(async (file, note, mode) => {
    setCurrentState(STATES.LOADING);
    setError(null);

    try {
      if (mode === 'refined') {
        // 精细模式
        const data = await startRefinedAnalysis(file, note);
        if (data.status === 'complete') {
          setResult(data.result);
          setCurrentState(STATES.RESULT);
        } else if (data.status === 'need_input') {
          setSessionId(data.sessionId);
          setCurrentQuestion(data.question);
          setPartialResult(data.partialResult);
          setCurrentState(STATES.QUESTIONING);
        }
      } else {
        // 粗略模式（不变）
        const data = await analyzeFood(file, note);
        setResult(data);
        setCurrentState(STATES.RESULT);
      }
    } catch (err) {
      setError(err.message || '分析失败，请重试');
      setCurrentState(STATES.UPLOAD);
    }
  }, []);

  const handleAnswerSubmit = useCallback(async (answer) => {
    setCurrentState(STATES.LOADING);

    try {
      const data = await continueRefinedAnalysis(sessionId, answer);
      if (data.status === 'complete') {
        setResult(data.result);
        setSessionId(null);
        setCurrentQuestion(null);
        setPartialResult(null);
        setCurrentState(STATES.RESULT);
      } else if (data.status === 'need_input') {
        setCurrentQuestion(data.question);
        setPartialResult(data.partialResult);
        setCurrentState(STATES.QUESTIONING);
      }
    } catch (err) {
      // 会话过期或其他错误，回到上传页
      setError(err.message || '分析失败，请重试');
      setSessionId(null);
      setCurrentQuestion(null);
      setPartialResult(null);
      setCurrentState(STATES.UPLOAD);
    }
  }, [sessionId]);

  const handleReset = useCallback(() => {
    setCurrentState(STATES.UPLOAD);
    setResult(null);
    setError(null);
    setSessionId(null);
    setCurrentQuestion(null);
    setPartialResult(null);
  }, []);

  const renderContent = () => {
    switch (currentState) {
      case STATES.UPLOAD:
        return (
          <UploadPage 
            onUpload={handleUpload} 
            initialError={error}
          />
        );
      case STATES.LOADING:
        return <LoadingPage />;
      case STATES.QUESTIONING:
        return (
          <QuestionPage
            question={currentQuestion}
            onSubmit={handleAnswerSubmit}
            partialResult={partialResult}
          />
        );
      case STATES.RESULT:
        return (
          <ResultPage 
            result={result} 
            onReset={handleReset} 
          />
        );
      default:
        return <UploadPage onUpload={handleUpload} />;
    }
  };

  return (
    <div className="app">
      <div className="container">
        <div className={`page-content page-${currentState}`}>
          {renderContent()}
        </div>
      </div>
    </div>
  );
}

export default App;
