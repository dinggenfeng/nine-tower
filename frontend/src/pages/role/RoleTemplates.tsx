import { useCallback, useEffect, useState } from "react";
import { Button, Card, Form, Input, Modal, Popconfirm, Space, Tree, message } from "antd";
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  FileTextOutlined,
  ArrowLeftOutlined,
  FolderOutlined,
  DownloadOutlined,
} from "@ant-design/icons";
import CodeMirror from "@uiw/react-codemirror";
import { yaml } from "@codemirror/lang-yaml";
import type {
  Template,
  CreateTemplateRequest,
  UpdateTemplateRequest,
} from "../../types/entity/Template";
import {
  createTemplate,
  getTemplates,
  getTemplate,
  updateTemplate,
  deleteTemplate,
  getTemplateDownloadUrl,
} from "../../api/template";

interface RoleTemplatesProps {
  roleId: number;
}

export default function RoleTemplates({ roleId }: RoleTemplatesProps) {
  const [templates, setTemplates] = useState<Template[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingTemplate, setEditingTemplate] = useState<Template | null>(null);
  const [contentTemplate, setContentTemplate] = useState<Template | null>(null);
  const [contentValue, setContentValue] = useState("");
  const [saving, setSaving] = useState(false);
  const [form] = Form.useForm();

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const list = await getTemplates(roleId);
      setTemplates(list);
    } finally {
      setLoading(false);
    }
  }, [roleId]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleCreate = () => {
    setEditingTemplate(null);
    form.resetFields();
    setModalOpen(true);
  };

  const handleEdit = (record: Template) => {
    setEditingTemplate(record);
    form.setFieldsValue({
      name: record.name,
      parentDir: record.parentDir,
      targetPath: record.targetPath,
    });
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      if (editingTemplate) {
        const data: UpdateTemplateRequest = {
          name: values.name,
          parentDir: values.parentDir,
          targetPath: values.targetPath,
        };
        await updateTemplate(editingTemplate.id, data);
        message.success("模板已更新");
      } else {
        const data: CreateTemplateRequest = {
          name: values.name,
          parentDir: values.parentDir,
          targetPath: values.targetPath,
        };
        await createTemplate(roleId, data);
        message.success("模板已创建");
      }
      setModalOpen(false);
      fetchData();
    } catch (err) {
      if (err && typeof err === "object" && "message" in err) {
        message.error((err as { message: string }).message);
      }
    }
  };

  const handleDelete = async (id: number) => {
    await deleteTemplate(id);
    message.success("模板已删除");
    fetchData();
  };

  const handleEditContent = async (record: Template) => {
    const detail = await getTemplate(record.id);
    setContentTemplate(detail);
    setContentValue(detail.content || "");
  };

  const handleDownload = (record: Template) => {
    const token = localStorage.getItem("token");
    const url = `${getTemplateDownloadUrl(record.id)}?token=${token}`;
    window.open(url, "_blank");
  };

  const handleSaveContent = async () => {
    if (!contentTemplate) return;
    setSaving(true);
    try {
      await updateTemplate(contentTemplate.id, { content: contentValue });
      message.success("模板内容已保存");
      setContentTemplate(null);
      fetchData();
    } catch (err) {
      if (err && typeof err === "object" && "message" in err) {
        message.error((err as { message: string }).message);
      }
    } finally {
      setSaving(false);
    }
  };

  // --- Content editor view ---
  if (contentTemplate) {
    return (
      <div>
        <div
          style={{
            marginBottom: 16,
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
          }}
        >
          <Button
            type="text"
            icon={<ArrowLeftOutlined />}
            onClick={() => setContentTemplate(null)}
            style={{ color: "#64748b", padding: "4px 8px" }}
          >
            返回模板列表
          </Button>
          <Button type="primary" onClick={handleSaveContent} loading={saving}>
            保存
          </Button>
        </div>
        <Card
          title={
            <Space>
              <FileTextOutlined />
              <span>
                {contentTemplate.parentDir ? `${contentTemplate.parentDir}/` : ""}
                {contentTemplate.name}
              </span>
            </Space>
          }
          size="small"
          style={{ marginBottom: 16 }}
        >
          <span style={{ color: "#64748b" }}>
            目标路径: {contentTemplate.targetPath || "未设置"}
          </span>
        </Card>
        <CodeMirror
          value={contentValue}
          onChange={(value) => setContentValue(value)}
          extensions={[yaml()]}
          height="500px"
          basicSetup={{
            lineNumbers: true,
            highlightActiveLine: true,
            bracketMatching: true,
          }}
        />
      </div>
    );
  }

  // --- Tree view ---
  // Build directory tree from flat template list (templates have parentDir for hierarchy)
  interface AntTreeNode {
    key: string;
    title: React.ReactNode;
    children?: AntTreeNode[];
  }

  const buildTreeData = (): AntTreeNode[] => {
    // Group templates by directory path
    const dirNodes = new Map<string, AntTreeNode>();
    const rootNodes: AntTreeNode[] = [];

    for (const t of templates) {
      const parts = t.parentDir ? t.parentDir.split("/").filter(Boolean) : [];
      let currentLevel = rootNodes;
      let pathSoFar = "";

      // Create directory nodes for each path segment
      for (const part of parts) {
        pathSoFar = pathSoFar ? `${pathSoFar}/${part}` : part;
        if (!dirNodes.has(pathSoFar)) {
          const dirNode: AntTreeNode = {
            key: `dir:${pathSoFar}`,
            title: (
              <Space>
                <FolderOutlined />
                <span>{part}</span>
              </Space>
            ),
            children: [],
          };
          dirNodes.set(pathSoFar, dirNode);
          currentLevel.push(dirNode);
        }
        const existing = dirNodes.get(pathSoFar)!;
        currentLevel = existing.children!;
      }

      // Add the template file node
      currentLevel.push({
        key: `file:${t.id}`,
        title: (
          <Space>
            <FileTextOutlined />
            <span>{t.name}</span>
            <Button
              type="link"
              size="small"
              onClick={(e) => {
                e.stopPropagation();
                handleEditContent(t);
              }}
            >
              内容
            </Button>
            <Button
              type="link"
              size="small"
              icon={<EditOutlined />}
              onClick={(e) => {
                e.stopPropagation();
                handleEdit(t);
              }}
            />
            <Button
              type="link"
              size="small"
              icon={<DownloadOutlined />}
              onClick={(e) => {
                e.stopPropagation();
                handleDownload(t);
              }}
            />
            <Popconfirm title="确定删除？" onConfirm={() => handleDelete(t.id)}>
              <Button
                type="link"
                size="small"
                danger
                icon={<DeleteOutlined />}
                onClick={(e) => e.stopPropagation()}
              />
            </Popconfirm>
          </Space>
        ),
      });
    }

    return rootNodes;
  };

  const antTreeData = buildTreeData();

  return (
    <div>
      <div style={{ marginBottom: 16 }}>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
          添加模板
        </Button>
      </div>
      {loading ? <span>加载中...</span> : <Tree treeData={antTreeData} defaultExpandAll />}
      <Modal
        title={editingTemplate ? "编辑模板" : "添加模板"}
        open={modalOpen}
        onOk={handleSubmit}
        onCancel={() => setModalOpen(false)}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="name"
            label="文件名"
            rules={[{ required: true, message: "请输入模板文件名" }]}
          >
            <Input placeholder="例如: nginx.conf.j2" />
          </Form.Item>
          <Form.Item name="parentDir" label="目录">
            <Input placeholder="例如: nginx/conf.d（留空表示根目录）" />
          </Form.Item>
          <Form.Item name="targetPath" label="目标路径">
            <Input placeholder="例如: /etc/nginx/nginx.conf" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
