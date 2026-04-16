import { Form, Input, Select, Switch, Button, Tooltip } from 'antd';
import { PlusOutlined, MinusCircleOutlined, QuestionCircleOutlined } from '@ant-design/icons';
import type { ModuleDefinition } from '../../constants/ansibleModules';
import { getModuleDefinition } from '../../constants/ansibleModules';

interface ModuleParamsFormProps {
  moduleName: string | undefined;
}

/** 渲染模块特定参数（用于放在双列 grid 中） */
export function ModuleParamsGrid({ moduleName }: ModuleParamsFormProps) {
  const moduleDef: ModuleDefinition | undefined = moduleName
    ? getModuleDefinition(moduleName)
    : undefined;

  if (!moduleDef) return null;

  return (
    <>
      {moduleDef.params.map((param) => (
        <Form.Item
          key={param.name}
          name={['moduleParams', param.name]}
          label={
            <span>
              {param.label}
              {param.tooltip && (
                <Tooltip title={param.tooltip}>
                  <QuestionCircleOutlined style={{ marginLeft: 4, color: '#999' }} />
                </Tooltip>
              )}
            </span>
          }
          rules={
            param.required
              ? [{ required: true, message: `请输入 ${param.label}` }]
              : undefined
          }
          valuePropName={param.type === 'switch' ? 'checked' : 'value'}
          initialValue={param.defaultValue}
        >
          {param.type === 'input' && <Input placeholder={param.placeholder} />}
          {param.type === 'select' && (
            <Select
              allowClear
              placeholder={param.placeholder || '请选择'}
              options={param.options}
            />
          )}
          {param.type === 'switch' && <Switch />}
        </Form.Item>
      ))}
    </>
  );
}

/** 渲染额外参数输入区域（独占一行，输入框与模块参数等宽） */
export function ExtraParamsInput() {
  return (
    <Form.List name="extraParams">
      {(fields, { add, remove }) => (
        <div
          style={{
            position: 'relative',
            height: fields.length * 48 + 56,
            minHeight: fields.length * 48 + 56,
          }}
        >
          {fields.map(({ key, name, ...restField }, index) => (
            <div
              key={key}
              style={{
                position: 'absolute',
                top: index * 48,
                left: 0,
                right: 0,
                display: 'flex',
                alignItems: 'center',
                gap: 8,
              }}
            >
              <Form.Item
                {...restField}
                name={[name, 'key']}
                rules={[{ required: true, message: '请输入参数名' }]}
                style={{ marginBottom: 0, flex: 1 }}
              >
                <Input placeholder="参数名" />
              </Form.Item>
              <Form.Item
                {...restField}
                name={[name, 'value']}
                rules={[{ required: true, message: '请输入参数值' }]}
                style={{ marginBottom: 0, flex: 1 }}
              >
                <Input placeholder="参数值" />
              </Form.Item>
              <MinusCircleOutlined
                onClick={() => remove(name)}
                style={{ cursor: 'pointer', color: '#ff4d4f', flexShrink: 0 }}
              />
            </div>
          ))}
          <Button
            type="dashed"
            onClick={() => add()}
            icon={<PlusOutlined />}
            style={{ position: 'absolute', top: fields.length * 48 + 8, width: 'fit-content' }}
          >
            添加参数
          </Button>
        </div>
      )}
    </Form.List>
  );
}
