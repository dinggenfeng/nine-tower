import { useEffect, useState, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import {
  Card,
  Select,
  Button,
  Space,
  message,
  Popconfirm,
  Spin,
} from 'antd';
import { DeleteOutlined, CopyOutlined } from '@ant-design/icons';
import {
  getPlaybook,
  addRole,
  removeRole,
  addHostGroup,
  removeHostGroup,
  addTag,
  removeTag,
  addEnvironment,
  removeEnvironment,
  generateYaml,
} from '../../api/playbook';
import { getRoles } from '../../api/role';
import { getHostGroups } from '../../api/host';
import { listTags } from '../../api/tag';
import { listEnvironments } from '../../api/environment';
import type { Playbook } from '../../types/entity/Playbook';
import type { Role } from '../../types/entity/Role';
import type { HostGroup } from '../../types/entity/Host';
import type { Tag } from '../../types/entity/Tag';
import type { Environment } from '../../types/entity/Environment';

export default function PlaybookBuilder() {
  const { id: projectId, pbId } = useParams<{
    id: string;
    pbId: string;
  }>();
  const pid = Number(projectId);
  const playbookId = Number(pbId);

  const [playbook, setPlaybook] = useState<Playbook | null>(null);
  const [roles, setRoles] = useState<Role[]>([]);
  const [hostGroups, setHostGroups] = useState<HostGroup[]>([]);
  const [tags, setTags] = useState<Tag[]>([]);
  const [environments, setEnvironments] = useState<Environment[]>([]);
  const [yamlPreview, setYamlPreview] = useState('');
  const [loading, setLoading] = useState(true);

  const fetchData = useCallback(async () => {
    if (!pid || !playbookId) return;
    setLoading(true);
    try {
      const [pb, rList, hgList, tList, envList, yaml] = await Promise.all([
        getPlaybook(playbookId),
        getRoles(pid),
        getHostGroups(pid),
        listTags(pid),
        listEnvironments(pid),
        generateYaml(playbookId),
      ]);
      setPlaybook(pb);
      setRoles(rList);
      setHostGroups(hgList);
      setTags(tList);
      setEnvironments(envList);
      setYamlPreview(yaml);
    } finally {
      setLoading(false);
    }
  }, [pid, playbookId]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleAddRole = async (roleId: number) => {
    await addRole(playbookId, roleId);
    message.success('Role 已添加');
    fetchData();
  };

  const handleRemoveRole = async (roleId: number) => {
    await removeRole(playbookId, roleId);
    fetchData();
  };

  const handleAddHostGroup = async (hgId: number) => {
    await addHostGroup(playbookId, hgId);
    fetchData();
  };

  const handleRemoveHostGroup = async (hgId: number) => {
    await removeHostGroup(playbookId, hgId);
    fetchData();
  };

  const handleAddTag = async (tagId: number) => {
    await addTag(playbookId, tagId);
    fetchData();
  };

  const handleRemoveTag = async (tagId: number) => {
    await removeTag(playbookId, tagId);
    fetchData();
  };

  const handleAddEnvironment = async (envId: number) => {
    await addEnvironment(playbookId, envId);
    fetchData();
  };

  const handleRemoveEnvironment = async (envId: number) => {
    await removeEnvironment(playbookId, envId);
    fetchData();
  };

  const handleCopyYaml = () => {
    navigator.clipboard.writeText(yamlPreview);
    message.success('已复制到剪贴板');
  };

  if (loading) return <Spin />;
  if (!playbook) return <div>Playbook not found</div>;

  const availableRoles = roles.filter(
    (r) => !playbook.roleIds.includes(r.id)
  );
  const availableHostGroups = hostGroups.filter(
    (h) => !playbook.hostGroupIds.includes(h.id)
  );
  const availableTags = tags.filter(
    (t) => !playbook.tagIds.includes(t.id)
  );
  const availableEnvironments = environments.filter(
    (e) => !playbook.environmentIds.includes(e.id)
  );
  const selectedRoles = playbook.roleIds
    .map((id) => roles.find((r) => r.id === id))
    .filter((r): r is Role => r != null);
  const selectedHostGroups = playbook.hostGroupIds
    .map((id) => hostGroups.find((h) => h.id === id))
    .filter((h): h is HostGroup => h != null);
  const selectedTags = playbook.tagIds
    .map((id) => tags.find((t) => t.id === id))
    .filter((t): t is Tag => t != null);
  const selectedEnvironments = playbook.environmentIds
    .map((id) => environments.find((e) => e.id === id))
    .filter((e): e is Environment => e != null);

  return (
    <div>
      <h2>{playbook.name}</h2>
      {playbook.description && <p>{playbook.description}</p>}

      <Space direction="vertical" style={{ width: '100%' }} size={16}>
        <Card title="主机组" size="small">
          <Space wrap>
            {selectedHostGroups.map((hg) => (
              <span key={hg.id}>
                <Space>
                  <strong>{hg.name}</strong>
                  <Popconfirm
                    title="确定移除？"
                    onConfirm={() => handleRemoveHostGroup(hg.id)}
                  >
                    <Button
                      type="text"
                      size="small"
                      danger
                      icon={<DeleteOutlined />}
                    />
                  </Popconfirm>
                </Space>
              </span>
            ))}
            {availableHostGroups.length > 0 && (
              <Select
                placeholder="添加主机组"
                style={{ width: 180 }}
                onSelect={(val: number) => handleAddHostGroup(val)}
                options={availableHostGroups.map((h) => ({
                  label: h.name,
                  value: h.id,
                }))}
              />
            )}
          </Space>
        </Card>

        <Card title="Roles（按顺序）" size="small">
          <Space direction="vertical" style={{ width: '100%' }}>
            {selectedRoles.map((r) => (
              <div
                key={r.id}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 8,
                }}
              >
                <span>{r.name}</span>
                <Popconfirm
                  title="确定移除？"
                  onConfirm={() => handleRemoveRole(r.id)}
                >
                  <Button
                    type="text"
                    size="small"
                    danger
                    icon={<DeleteOutlined />}
                  />
                </Popconfirm>
              </div>
            ))}
            {availableRoles.length > 0 && (
              <Select
                placeholder="添加 Role"
                style={{ width: 200 }}
                onSelect={(val: number) => handleAddRole(val)}
                options={availableRoles.map((r) => ({
                  label: r.name,
                  value: r.id,
                }))}
              />
            )}
          </Space>
        </Card>

        <Card title="标签" size="small">
          <Space wrap>
            {selectedTags.map((t) => (
              <span key={t.id}>
                <Space>
                  <strong>{t.name}</strong>
                  <Popconfirm
                    title="确定移除？"
                    onConfirm={() => handleRemoveTag(t.id)}
                  >
                    <Button
                      type="text"
                      size="small"
                      danger
                      icon={<DeleteOutlined />}
                    />
                  </Popconfirm>
                </Space>
              </span>
            ))}
            {availableTags.length > 0 && (
              <Select
                placeholder="添加标签"
                style={{ width: 160 }}
                onSelect={(val: number) => handleAddTag(val)}
                options={availableTags.map((t) => ({
                  label: t.name,
                  value: t.id,
                }))}
              />
            )}
          </Space>
        </Card>

        <Card title="环境" size="small">
          <Space wrap>
            {selectedEnvironments.map((e) => (
              <span key={e.id}>
                <Space>
                  <strong>{e.name}</strong>
                  <Popconfirm
                    title="确定移除？"
                    onConfirm={() => handleRemoveEnvironment(e.id)}
                  >
                    <Button
                      type="text"
                      size="small"
                      danger
                      icon={<DeleteOutlined />}
                    />
                  </Popconfirm>
                </Space>
              </span>
            ))}
            {availableEnvironments.length > 0 && (
              <Select
                placeholder="添加环境"
                style={{ width: 180 }}
                onSelect={(val: number) => handleAddEnvironment(val)}
                options={availableEnvironments.map((e) => ({
                  label: e.name,
                  value: e.id,
                }))}
              />
            )}
          </Space>
        </Card>

        <Card
          title="YAML 预览"
          size="small"
          extra={
            <Button
              icon={<CopyOutlined />}
              onClick={handleCopyYaml}
            >
              复制
            </Button>
          }
        >
          <pre
            style={{
              background: '#f5f5f5',
              padding: 12,
              borderRadius: 4,
              overflow: 'auto',
              fontSize: 13,
            }}
          >
            {yamlPreview || '---'}
          </pre>
        </Card>
      </Space>
    </div>
  );
}
