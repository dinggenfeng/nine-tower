import { useState, useEffect, useCallback } from 'react';
import { Button, Card, Collapse, Form, Input, Switch, Tabs, Space, Popconfirm } from 'antd';
import { PlusOutlined, DeleteOutlined, UpOutlined, DownOutlined, MinusCircleOutlined } from '@ant-design/icons';
import type { BlockChildRequest, BlockSection } from '../../types/entity/Task';
import ModuleSelect from './ModuleSelect';

interface BlockChildFormData {
  key: string;
  name: string;
  module: string;
  extraParams: { key: string; value: string }[];
  whenCondition: string;
  loop: string;
  register: string;
  become: boolean;
  becomeUser: string;
  ignoreErrors: boolean;
  _section: BlockSection;
}

function generateKey(): string {
  return Math.random().toString(36).slice(2);
}

function ExtraParamsEditor({
  extraParams,
  onChange,
}: {
  extraParams: { key: string; value: string }[];
  onChange: (ep: { key: string; value: string }[]) => void;
}) {
  return (
    <div style={{ marginBottom: 12 }}>
      <div style={{ color: 'rgba(0,0,0,0.88)', fontSize: 14, marginBottom: 8 }}>额外参数</div>
      {extraParams.map((item, idx) => (
        <div key={idx} style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
          <Input
            placeholder="参数名"
            value={item.key}
            onChange={(e) => {
              const updated = [...extraParams];
              updated[idx] = { ...updated[idx], key: e.target.value };
              onChange(updated);
            }}
            style={{ flex: 1 }}
          />
          <Input
            placeholder="参数值"
            value={item.value}
            onChange={(e) => {
              const updated = [...extraParams];
              updated[idx] = { ...updated[idx], value: e.target.value };
              onChange(updated);
            }}
            style={{ flex: 1 }}
          />
          <MinusCircleOutlined
            onClick={() => onChange(extraParams.filter((_, i) => i !== idx))}
            style={{ cursor: 'pointer', color: '#ff4d4f', flexShrink: 0 }}
          />
        </div>
      ))}
      <Button
        type="dashed"
        onClick={() => onChange([...extraParams, { key: '', value: '' }])}
        icon={<PlusOutlined />}
        style={{ width: 'fit-content' }}
      >
        添加参数
      </Button>
    </div>
  );
}

function ChildTaskCard({
  data,
  index,
  total,
  onChange,
  onDelete,
  onMoveUp,
  onMoveDown,
}: {
  data: BlockChildFormData;
  index: number;
  total: number;
  onChange: (d: BlockChildFormData) => void;
  onDelete: () => void;
  onMoveUp: () => void;
  onMoveDown: () => void;
}) {
  return (
    <Card
      size="small"
      style={{ marginBottom: 8 }}
      title={
        <Space>
          <span>{index + 1}. {data.name || '(未命名子任务)'}</span>
          {data.module && <span style={{ color: '#999' }}>({data.module})</span>}
        </Space>
      }
      extra={
        <Space>
          <Button type="text" size="small" icon={<UpOutlined />} disabled={index === 0} onClick={onMoveUp} />
          <Button type="text" size="small" icon={<DownOutlined />} disabled={index === total - 1} onClick={onMoveDown} />
          <Popconfirm title="确定删除?" onConfirm={onDelete}>
            <Button type="text" size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      }
    >
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0 16px' }}>
        <Form.Item label="名称" required style={{ marginBottom: 12 }}>
          <Input value={data.name} onChange={(e) => onChange({ ...data, name: e.target.value })} placeholder="子任务名称" />
        </Form.Item>
        <Form.Item label="模块" required style={{ marginBottom: 12 }}>
          <ModuleSelect
            value={data.module}
            onChange={(val) => onChange({ ...data, module: val || '', extraParams: [] })}
            filterModule="block"
          />
        </Form.Item>
      </div>
      {data.module && (
        <ExtraParamsEditor
          extraParams={data.extraParams}
          onChange={(ep) => onChange({ ...data, extraParams: ep })}
        />
      )}
      <Collapse
        ghost
        bordered={false}
        items={[{
          key: 'advanced',
          label: '高级选项',
          children: (
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0 16px' }}>
              <Form.Item label="When" style={{ marginBottom: 8 }}>
                <Input value={data.whenCondition} onChange={(e) => onChange({ ...data, whenCondition: e.target.value })} placeholder="条件表达式" />
              </Form.Item>
              <Form.Item label="Loop" style={{ marginBottom: 8 }}>
                <Input value={data.loop} onChange={(e) => onChange({ ...data, loop: e.target.value })} placeholder="{{ items }}" />
              </Form.Item>
              <Form.Item label="Register" style={{ marginBottom: 8 }}>
                <Input value={data.register} onChange={(e) => onChange({ ...data, register: e.target.value })} placeholder="变量名" />
              </Form.Item>
              <Form.Item label="Become" style={{ marginBottom: 8 }}>
                <Switch checked={data.become} onChange={(v) => onChange({ ...data, become: v })} />
              </Form.Item>
              <Form.Item label="Become User" style={{ marginBottom: 8 }}>
                <Input value={data.becomeUser} onChange={(e) => onChange({ ...data, becomeUser: e.target.value })} placeholder="root" />
              </Form.Item>
              <Form.Item label="Ignore Errors" style={{ marginBottom: 8 }}>
                <Switch checked={data.ignoreErrors} onChange={(v) => onChange({ ...data, ignoreErrors: v })} />
              </Form.Item>
            </div>
          ),
        }]}
      />
    </Card>
  );
}

export default function BlockTasksEditor({
  blockChildren,
  onChange,
}: {
  blockChildren: BlockChildRequest[];
  onChange: (children: BlockChildRequest[]) => void;
}) {
  const [childForms, setChildForms] = useState<BlockChildFormData[]>(() =>
    blockChildren.length > 0
      ? blockChildren.map((c) => ({
          key: generateKey(),
          name: c.name,
          module: c.module,
          extraParams: parseArgsToExtraParams(c.args),
          whenCondition: c.whenCondition || '',
          loop: c.loop || '',
          register: c.register || '',
          become: c.become || false,
          becomeUser: c.becomeUser || '',
          ignoreErrors: c.ignoreErrors || false,
          _section: c.section,
        }))
      : []
  );

  const syncToParent = useCallback(() => {
    const result: BlockChildRequest[] = childForms
      .filter((f) => f.name && f.module)
      .map((f, i) => ({
        section: f._section,
        name: f.name,
        module: f.module,
        args: buildArgsJson(f.extraParams),
        whenCondition: f.whenCondition || undefined,
        loop: f.loop || undefined,
        register: f.register || undefined,
        become: f.become || undefined,
        becomeUser: f.becomeUser || undefined,
        ignoreErrors: f.ignoreErrors || undefined,
        taskOrder: i,
      }));
    onChange(result);
  }, [childForms, onChange]);

  useEffect(() => {
    syncToParent();
  }, [syncToParent]);

  const getFormsBySection = (section: BlockSection) =>
    childForms.filter((f) => f._section === section);

  const handleAddChild = (section: BlockSection) => {
    setChildForms((prev) => [
      ...prev,
      {
        key: generateKey(),
        name: '',
        module: '',
        extraParams: [],
        whenCondition: '',
        loop: '',
        register: '',
        become: false,
        becomeUser: '',
        ignoreErrors: false,
        _section: section,
      },
    ]);
  };

  const handleUpdateChild = (key: string, updated: BlockChildFormData) => {
    setChildForms((prev) => prev.map((f) => (f.key === key ? updated : f)));
  };

  const handleDeleteChild = (key: string) => {
    setChildForms((prev) => prev.filter((f) => f.key !== key));
  };

  const handleMoveUp = (section: BlockSection, idx: number) => {
    const forms = getFormsBySection(section);
    if (idx <= 0) return;
    const otherForms = childForms.filter((f) => f._section !== section);
    const reordered = [...forms];
    [reordered[idx - 1], reordered[idx]] = [reordered[idx], reordered[idx - 1]];
    setChildForms([...otherForms, ...reordered.map((f) => ({ ...f, _section: section as BlockSection }))]);
  };

  const handleMoveDown = (section: BlockSection, idx: number) => {
    const forms = getFormsBySection(section);
    if (idx >= forms.length - 1) return;
    const otherForms = childForms.filter((f) => f._section !== section);
    const reordered = [...forms];
    [reordered[idx], reordered[idx + 1]] = [reordered[idx + 1], reordered[idx]];
    setChildForms([...otherForms, ...reordered.map((f) => ({ ...f, _section: section as BlockSection }))]);
  };

  const tabItems = [
    {
      key: 'BLOCK',
      label: 'block（必填）',
      children: (
        <div>
          {getFormsBySection('BLOCK').length === 0 && (
            <div style={{ color: '#999', textAlign: 'center', padding: 8 }}>暂无 block 子任务，请添加</div>
          )}
          {getFormsBySection('BLOCK').map((form, i) => (
            <ChildTaskCard
              key={form.key}
              data={form}
              index={i}
              total={getFormsBySection('BLOCK').length}
              onChange={(d) => handleUpdateChild(form.key, d)}
              onDelete={() => handleDeleteChild(form.key)}
              onMoveUp={() => handleMoveUp('BLOCK', i)}
              onMoveDown={() => handleMoveDown('BLOCK', i)}
            />
          ))}
          <Button type="dashed" icon={<PlusOutlined />} onClick={() => handleAddChild('BLOCK')} style={{ width: '100%', marginTop: 8 }}>
            添加 block 子任务
          </Button>
        </div>
      ),
    },
    {
      key: 'RESCUE',
      label: 'rescue（可选）',
      children: (
        <div>
          {getFormsBySection('RESCUE').length === 0 && (
            <div style={{ color: '#999', textAlign: 'center', padding: 8 }}>暂无 rescue 任务（可选）</div>
          )}
          {getFormsBySection('RESCUE').map((form, i) => (
            <ChildTaskCard
              key={form.key}
              data={form}
              index={i}
              total={getFormsBySection('RESCUE').length}
              onChange={(d) => handleUpdateChild(form.key, d)}
              onDelete={() => handleDeleteChild(form.key)}
              onMoveUp={() => handleMoveUp('RESCUE', i)}
              onMoveDown={() => handleMoveDown('RESCUE', i)}
            />
          ))}
          <Button type="dashed" icon={<PlusOutlined />} onClick={() => handleAddChild('RESCUE')} style={{ width: '100%', marginTop: 8 }}>
            添加 rescue 子任务
          </Button>
        </div>
      ),
    },
    {
      key: 'ALWAYS',
      label: 'always（可选）',
      children: (
        <div>
          {getFormsBySection('ALWAYS').length === 0 && (
            <div style={{ color: '#999', textAlign: 'center', padding: 8 }}>暂无 always 任务（可选）</div>
          )}
          {getFormsBySection('ALWAYS').map((form, i) => (
            <ChildTaskCard
              key={form.key}
              data={form}
              index={i}
              total={getFormsBySection('ALWAYS').length}
              onChange={(d) => handleUpdateChild(form.key, d)}
              onDelete={() => handleDeleteChild(form.key)}
              onMoveUp={() => handleMoveUp('ALWAYS', i)}
              onMoveDown={() => handleMoveDown('ALWAYS', i)}
            />
          ))}
          <Button type="dashed" icon={<PlusOutlined />} onClick={() => handleAddChild('ALWAYS')} style={{ width: '100%', marginTop: 8 }}>
            添加 always 子任务
          </Button>
        </div>
      ),
    },
  ];

  return <Tabs items={tabItems} />;
}

function buildArgsJson(
  extraParams: { key: string; value: string }[] | undefined,
): string {
  const result: Record<string, unknown> = {};
  if (extraParams) {
    for (const item of extraParams) {
      if (item.key) {
        result[item.key] = item.value;
      }
    }
  }
  return Object.keys(result).length > 0 ? JSON.stringify(result) : '';
}

function parseArgsToExtraParams(args: string | undefined): { key: string; value: string }[] {
  if (!args) return [];
  try {
    const parsed = JSON.parse(args);
    return Object.entries(parsed).map(([key, value]) => ({
      key,
      value: String(value),
    }));
  } catch {
    return [];
  }
}
