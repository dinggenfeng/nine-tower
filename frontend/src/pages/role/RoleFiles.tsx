import { useCallback, useEffect, useState } from "react";
import { Button, Form, Input, Modal, Popconfirm, Space, Tree, message } from "antd";
import {
  PlusOutlined,
  FolderOutlined,
  FileOutlined,
  DownloadOutlined,
  EditOutlined,
  DeleteOutlined,
} from "@ant-design/icons";
import type { RoleFile, CreateFileRequest } from "../../types/entity/RoleFile";
import {
  getFiles,
  createFile,
  updateFile,
  deleteFile,
  downloadFile,
} from "../../api/roleFile";

interface RoleFilesProps {
  roleId: number;
}

export default function RoleFiles({ roleId }: RoleFilesProps) {
  const [files, setFiles] = useState<RoleFile[]>([]);
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [selectedFile, setSelectedFile] = useState<RoleFile | null>(null);
  const [creatingDir, setCreatingDir] = useState(false);
  const [createForm] = Form.useForm();
  const [editForm] = Form.useForm();

  const fetchData = useCallback(async () => {
    try {
      const list = await getFiles(roleId);
      setFiles(list);
    } catch {
      // ignore fetch errors
    }
  }, [roleId]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleCreate = (parentDir: string, isDir: boolean) => {
    setCreatingDir(isDir);
    createForm.resetFields();
    createForm.setFieldsValue({ parentDir, name: "" });
    setCreateModalOpen(true);
  };

  const handleCreateSubmit = async () => {
    try {
      const values = await createForm.validateFields();
      const data: CreateFileRequest = {
        parentDir: values.parentDir,
        name: values.name,
        isDirectory: creatingDir,
        textContent: creatingDir ? undefined : values.textContent,
      };
      await createFile(roleId, data);
      message.success(creatingDir ? "目录已创建" : "文件已创建");
      setCreateModalOpen(false);
      fetchData();
    } catch (err) {
      if (err && typeof err === "object" && "message" in err) {
        message.error((err as { message: string }).message);
      }
    }
  };

  const handleEdit = (file: RoleFile) => {
    setSelectedFile(file);
    editForm.setFieldsValue({
      name: file.name,
      textContent: file.textContent || "",
    });
    setEditModalOpen(true);
  };

  const handleEditSubmit = async () => {
    try {
      const values = await editForm.validateFields();
      if (!selectedFile) return;
      await updateFile(selectedFile.id, {
        name: values.name,
        textContent: values.textContent,
      });
      message.success("更新成功");
      setEditModalOpen(false);
      fetchData();
    } catch (err) {
      if (err && typeof err === "object" && "message" in err) {
        message.error((err as { message: string }).message);
      }
    }
  };

  const handleDelete = async (fileId: number) => {
    await deleteFile(fileId);
    message.success("删除成功");
    fetchData();
  };

  const handleDownload = async (fileId: number) => {
    try {
      await downloadFile(fileId);
    } catch {
      message.error("下载失败");
    }
  };

  interface TreeNode {
    key: number;
    title: React.ReactNode;
    children?: TreeNode[];
  }

  const toTreeData = (fileList: RoleFile[]): TreeNode[] =>
    fileList.map((f) => ({
      key: f.id,
      title: (
        <Space>
          {f.isDirectory ? <FolderOutlined /> : <FileOutlined />}
          <span>{f.name}</span>
          {!f.isDirectory && f.size != null && f.size > 0 && (
            <span style={{ color: "#94a3b8", fontSize: 12 }}>
              ({(f.size / 1024).toFixed(1)} KB)
            </span>
          )}
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={(e) => {
              e.stopPropagation();
              handleEdit(f);
            }}
          />
          {f.isDirectory && (
            <Button
              type="link"
              size="small"
              icon={<PlusOutlined />}
              onClick={(e) => {
                e.stopPropagation();
                const dir = f.parentDir ? `${f.parentDir}/${f.name}` : f.name;
                handleCreate(dir, false);
              }}
            />
          )}
          {!f.isDirectory && (
            <Button
              type="link"
              size="small"
              icon={<DownloadOutlined />}
              onClick={(e) => {
                e.stopPropagation();
                handleDownload(f.id);
              }}
            />
          )}
          <Popconfirm title="确定删除？" onConfirm={() => handleDelete(f.id)}>
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
      children: f.children ? toTreeData(f.children) : undefined,
    }));

  return (
    <div>
      <div style={{ marginBottom: 16 }}>
        <Space>
          <Button icon={<PlusOutlined />} onClick={() => handleCreate("", false)}>
            新建文件
          </Button>
          <Button icon={<PlusOutlined />} onClick={() => handleCreate("", true)}>
            新建目录
          </Button>
        </Space>
      </div>

      <Tree treeData={toTreeData(files)} defaultExpandAll />

      <Modal
        title={creatingDir ? "新建目录" : "新建文件"}
        open={createModalOpen}
        onOk={handleCreateSubmit}
        onCancel={() => setCreateModalOpen(false)}
      >
        <Form form={createForm} layout="vertical">
          <Form.Item name="parentDir" hidden>
            <Input />
          </Form.Item>
          <Form.Item name="name" label="名称" rules={[{ required: true, message: "请输入名称" }]}>
            <Input placeholder={creatingDir ? "目录名称" : "文件名称"} />
          </Form.Item>
          {!creatingDir && (
            <Form.Item name="textContent" label="文件内容">
              <Input.TextArea rows={8} placeholder="文件内容（可选，创建后可编辑）" />
            </Form.Item>
          )}
        </Form>
      </Modal>

      <Modal
        title="编辑文件"
        open={editModalOpen}
        onOk={handleEditSubmit}
        onCancel={() => setEditModalOpen(false)}
        width={640}
      >
        <Form form={editForm} layout="vertical">
          <Form.Item
            name="name"
            label="文件名"
            rules={[{ required: true, message: "请输入文件名" }]}
          >
            <Input />
          </Form.Item>
          {selectedFile && !selectedFile.isDirectory && (
            <Form.Item name="textContent" label="内容">
              <Input.TextArea rows={15} style={{ fontFamily: "monospace" }} />
            </Form.Item>
          )}
        </Form>
      </Modal>
    </div>
  );
}
