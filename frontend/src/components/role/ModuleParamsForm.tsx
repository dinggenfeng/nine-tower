import { Form, Input, Select, Switch, Button, Space, Tooltip } from 'antd';
import { PlusOutlined, MinusCircleOutlined, QuestionCircleOutlined } from '@ant-design/icons';
import type { ModuleDefinition } from '../../constants/ansibleModules';
import { getModuleDefinition } from '../../constants/ansibleModules';

interface ModuleParamsFormProps {
  moduleName: string | undefined;
}

export default function ModuleParamsForm({ moduleName }: ModuleParamsFormProps) {
  const moduleDef: ModuleDefinition | undefined = moduleName
    ? getModuleDefinition(moduleName)
    : undefined;

  return (
    <>
      {moduleDef && (
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
      )}

      <Form.List name="extraParams">
        {(fields, { add, remove }) => (
          <>
            {fields.map(({ key, name, ...restField }) => (
              <Space key={key} style={{ display: 'flex', marginBottom: 8 }} align="baseline">
                <Form.Item
                  {...restField}
                  name={[name, 'key']}
                  rules={[{ required: true, message: '请输入参数名' }]}
                  style={{ marginBottom: 0 }}
                >
                  <Input placeholder="参数名" style={{ width: 180 }} />
                </Form.Item>
                <Form.Item
                  {...restField}
                  name={[name, 'value']}
                  rules={[{ required: true, message: '请输入参数值' }]}
                  style={{ marginBottom: 0 }}
                >
                  <Input placeholder="参数值" style={{ width: 280 }} />
                </Form.Item>
                <MinusCircleOutlined onClick={() => remove(name)} />
              </Space>
            ))}
            <Form.Item>
              <Button type="dashed" onClick={() => add()} block icon={<PlusOutlined />}>
                添加参数
              </Button>
            </Form.Item>
          </>
        )}
      </Form.List>
    </>
  );
}
