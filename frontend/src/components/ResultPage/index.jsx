import './ResultPage.css';

const FOOD_ICONS = {
  '米饭': '🍚',
  '面条': '🍜',
  '面包': '🍞',
  '肉': '🥩',
  '鸡肉': '🍗',
  '鱼': '🐟',
  '蔬菜': '🥬',
  '水果': '🍎',
  '蛋': '🥚',
  '奶': '🥛',
  'default': '🍽️'
};

function getFoodIcon(name) {
  for (const [key, icon] of Object.entries(FOOD_ICONS)) {
    if (name.includes(key)) return icon;
  }
  return FOOD_ICONS.default;
}

function CalorieRangeBar({ low, mid, high }) {
  const max = Math.max(low, mid, high, 800);
  const lowPercent = (low / max) * 100;
  const midPercent = (mid / max) * 100;
  const highPercent = (high / max) * 100;

  return (
    <div className="calorie-bar-container">
      <div className="calorie-bar">
        <div 
          className="calorie-bar-fill calorie-bar-low" 
          style={{ width: `${lowPercent}%` }}
        />
        <div 
          className="calorie-bar-fill calorie-bar-mid" 
          style={{ width: `${midPercent - lowPercent}%`, left: `${lowPercent}%` }}
        />
        <div 
          className="calorie-bar-fill calorie-bar-high" 
          style={{ width: `${highPercent - midPercent}%`, left: `${midPercent}%` }}
        />
      </div>
      <div className="calorie-bar-labels">
        <span>{low}</span>
        <span>{mid}</span>
        <span>{high}</span>
      </div>
    </div>
  );
}

function FoodItem({ food }) {
  const { name, estimatedWeight, calories } = food;
  const icon = getFoodIcon(name);

  return (
    <div className="food-item">
      <div className="food-icon">{icon}</div>
      <div className="food-info">
        <div className="food-name">{name}</div>
        <div className="food-weight">{estimatedWeight}</div>
      </div>
      <div className="food-calories">
        <div className="food-calories-value">{calories.mid}</div>
        <div className="food-calories-unit">千卡</div>
      </div>
    </div>
  );
}

function ResultPage({ result, onReset }) {
  const { foods, totalCalories, disclaimer } = result;

  if (!foods || foods.length === 0) {
    return (
      <div className="result-page">
        <div className="card empty-result">
          <div className="empty-icon">🔍</div>
          <h2 className="empty-title">未识别到食物</h2>
          <p className="empty-subtitle">请尝试上传更清晰的食物图片</p>
          <button className="btn btn-primary" onClick={onReset}>
            重新上传
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="result-page">
      <div className="header">
        <h1 className="header-title">分析结果 🔥</h1>
      </div>

      <div className="card total-calories-card">
        <div className="total-calories-label">总热量估算</div>
        <div className="total-calories-value">{totalCalories.mid}</div>
        <div className="total-calories-unit">千卡</div>
        <CalorieRangeBar 
          low={totalCalories.low} 
          mid={totalCalories.mid} 
          high={totalCalories.high} 
        />
        <div className="calories-range-text">
          区间：{totalCalories.low} - {totalCalories.high} 千卡
        </div>
      </div>

      <div className="card food-list-card">
        <h3 className="food-list-title">识别到的食物</h3>
        <div className="food-list">
          {foods.map((food, index) => (
            <FoodItem key={index} food={food} />
          ))}
        </div>
      </div>

      <button className="btn btn-primary" style={{ width: '100%' }} onClick={onReset}>
        <span>📷</span> 再拍一张
      </button>

      {disclaimer && (
        <div className="disclaimer">
          <p>💡 {disclaimer}</p>
        </div>
      )}
    </div>
  );
}

export default ResultPage;
