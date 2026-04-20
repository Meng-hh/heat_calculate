import { useState, useCallback } from 'react';
import UploadPage from './components/UploadPage';
import LoadingPage from './components/LoadingPage';
import ResultPage from './components/ResultPage';
import { analyzeFood } from './api';
import './styles/global.css';
import './App.css';

// App states
const STATES = {
  UPLOAD: 'upload',
  LOADING: 'loading',
  RESULT: 'result',
};

function App() {
  const [currentState, setCurrentState] = useState(STATES.UPLOAD);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);

  const handleUpload = useCallback(async (file, note) => {
    setCurrentState(STATES.LOADING);
    setError(null);

    try {
      const data = await analyzeFood(file, note);
      setResult(data);
      setCurrentState(STATES.RESULT);
    } catch (err) {
      setError(err.message || '分析失败，请重试');
      setCurrentState(STATES.UPLOAD);
    }
  }, []);

  const handleReset = useCallback(() => {
    setCurrentState(STATES.UPLOAD);
    setResult(null);
    setError(null);
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
