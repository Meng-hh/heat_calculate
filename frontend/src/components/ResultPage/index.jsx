import { useState, useCallback } from 'react';
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

function FoodItem({ food, index, editingIndex, onStartEdit, onConfirmEdit, onCancelEdit, editValues, onEditChange }) {
  const { name, estimatedWeight, calories } = food;
  const icon = getFoodIcon(name);
  const isEditing = editingIndex === index;

  if (isEditing) {
    return (
      <div className="food-item food-item-editing">
        <div className="food-icon">{icon}</div>
        <div className="food-edit-fields">
          <input
            type="text"
            className="food-edit-input"
            value={editValues.name}
            onChange={(e) => onEditChange('name', e.target.value)}
            placeholder="食物名称"
          />
          <input
            type="text"
            className="food-edit-input"
            value={editValues.weight}
            onChange={(e) => onEditChange('weight', e.target.value)}
            placeholder="重量，如200g"
          />
        </div>
        <div className="food-edit-actions">
          <button className="btn-edit-confirm" onClick={() => onConfirmEdit(index)}>✓</button>
          <button className="btn-edit-cancel" onClick={onCancelEdit}>✗</button>
        </div>
      </div>
    );
  }

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
      <button className="btn-food-edit" onClick={() => onStartEdit(index)}>
        ✏️
      </button>
    </div>
  );
}

function ResultPage({ result, onReset, sessionId, onCorrect }) {
  const { foods, totalCalories, disclaimer } = result;
  
  // 编辑状态管理
  const [editingIndex, setEditingIndex] = useState(-1);
  const [editValues, setEditValues] = useState({ name: '', weight: '' });
  const [corrections, setCorrections] = useState([]);
  const [additionalNote, setAdditionalNote] = useState('');

  const handleStartEdit = useCallback((index) => {
    setEditingIndex(index);
    setEditValues({
      name: foods[index].name,
      weight: foods[index].estimatedWeight,
    });
  }, [foods]);

  const handleEditChange = useCallback((field, value) => {
    setEditValues(prev => ({ ...prev, [field]: value }));
  }, []);

  const handleConfirmEdit = useCallback((index) => {
    const original = foods[index];
    const hasNameChange = editValues.name !== original.name;
    const hasWeightChange = editValues.weight !== original.estimatedWeight;

    if (hasNameChange || hasWeightChange) {
      setCorrections(prev => {
        // 替换已有的同 index 纠正项
        const filtered = prev.filter(c => c.index !== index);
        return [...filtered, {
          index,
          name: hasNameChange ? editValues.name : '',
          weight: hasWeightChange ? editValues.weight : '',
        }];
      });
    }
    setEditingIndex(-1);
  }, [foods, editValues]);

  const handleCancelEdit = useCallback(() => {
    setEditingIndex(-1);
    setEditValues({ name: '', weight: '' });
  }, []);

  const handleRecalculate = useCallback(() => {
    if (onCorrect && sessionId) {
      onCorrect(sessionId, corrections, additionalNote);
      // 重置编辑状态
      setCorrections([]);
      setAdditionalNote('');
      setEditingIndex(-1);
    }
  }, [onCorrect, sessionId, corrections, additionalNote]);

  const canRecalculate = corrections.length > 0 || additionalNote.trim().length > 0;

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
            <FoodItem
              key={index}
              food={food}
              index={index}
              editingIndex={editingIndex}
              onStartEdit={handleStartEdit}
              onConfirmEdit={handleConfirmEdit}
              onCancelEdit={handleCancelEdit}
              editValues={editValues}
              onEditChange={handleEditChange}
            />
          ))}
        </div>

        {corrections.length > 0 && (
          <div className="corrections-summary">
            <span className="corrections-badge">{corrections.length} 项已修改</span>
          </div>
        )}
      </div>

      {/* 补充备注 */}
      {sessionId && (
        <div className="card note-card">
          <textarea
            className="correction-note"
            value={additionalNote}
            onChange={(e) => setAdditionalNote(e.target.value)}
            placeholder="补充说明（可选），如：少油少盐、半份量..."
            rows={2}
          />
        </div>
      )}

      {/* 重新计算按钮 */}
      {sessionId && (
        <button
          className={`btn btn-accent recalculate-btn ${!canRecalculate ? 'btn-disabled' : ''}`}
          onClick={handleRecalculate}
          disabled={!canRecalculate}
        >
          🔄 重新计算
        </button>
      )}

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
