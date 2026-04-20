const API_BASE_URL = '';
const REQUEST_TIMEOUT = 30000; // 30 seconds

function getErrorMessage(status) {
  switch (status) {
    case 400:
      return '图片格式错误或大小超过限制，请检查图片';
    case 502:
      return 'AI 服务暂时不可用，请稍后重试';
    case 500:
      return '系统内部错误，请稍后重试';
    default:
      return '网络错误，请检查网络连接';
  }
}

async function fetchWithTimeout(url, options, timeout) {
  return Promise.race([
    fetch(url, options),
    new Promise((_, reject) =>
      setTimeout(() => reject(new Error('请求超时，请稍后重试')), timeout)
    ),
  ]);
}

/**
 * 分析食物图片
 * @param {File} image - 图片文件
 * @param {string} note - 可选备注
 * @returns {Promise<Object>} 分析结果
 */
export async function analyzeFood(image, note = '') {
  const formData = new FormData();
  formData.append('image', image);
  if (note && note.trim()) {
    formData.append('note', note.trim());
  }

  try {
    const response = await fetchWithTimeout(
      `${API_BASE_URL}/api/v1/calories/analyze`,
      {
        method: 'POST',
        body: formData,
      },
      REQUEST_TIMEOUT
    );

    if (!response.ok) {
      const errorMessage = getErrorMessage(response.status);
      throw new Error(errorMessage);
    }

    const data = await response.json();
    return data;
  } catch (error) {
    if (error.message === 'Failed to fetch') {
      throw new Error('无法连接到服务器，请检查网络');
    }
    throw error;
  }
}

/**
 * 开始精细分析
 * @param {File} image - 图片文件
 * @param {string} note - 可选备注
 * @returns {Promise<Object>} { sessionId, status, question?, partialResult?, result? }
 */
export async function startRefinedAnalysis(image, note = '') {
  const formData = new FormData();
  formData.append('image', image);
  if (note && note.trim()) {
    formData.append('note', note.trim());
  }

  try {
    const response = await fetchWithTimeout(
      `${API_BASE_URL}/api/v1/calories/analyze/refined`,
      {
        method: 'POST',
        body: formData,
      },
      REQUEST_TIMEOUT
    );

    if (!response.ok) {
      const errorMessage = getErrorMessage(response.status);
      throw new Error(errorMessage);
    }

    return await response.json();
  } catch (error) {
    if (error.message === 'Failed to fetch') {
      throw new Error('无法连接到服务器，请检查网络');
    }
    throw error;
  }
}

/**
 * 继续精细分析（提交用户回答）
 * @param {string} sessionId - 会话 ID
 * @param {string} answer - 用户回答
 * @returns {Promise<Object>} { sessionId, status, question?, partialResult?, result? }
 */
export async function continueRefinedAnalysis(sessionId, answer) {
  try {
    const response = await fetchWithTimeout(
      `${API_BASE_URL}/api/v1/calories/analyze/refined/${sessionId}/continue`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ answer }),
      },
      REQUEST_TIMEOUT
    );

    if (response.status === 404) {
      throw new Error('会话已过期，请重新上传图片');
    }

    if (!response.ok) {
      const errorMessage = getErrorMessage(response.status);
      throw new Error(errorMessage);
    }

    return await response.json();
  } catch (error) {
    if (error.message === 'Failed to fetch') {
      throw new Error('无法连接到服务器，请检查网络');
    }
    throw error;
  }
}

/**
 * 检查后端服务是否可用
 * @returns {Promise<boolean>}
 */
export async function checkHealth() {
  try {
    const response = await fetchWithTimeout(
      `${API_BASE_URL}/actuator/health`,
      { method: 'GET' },
      5000
    );
    return response.ok;
  } catch {
    return false;
  }
}
