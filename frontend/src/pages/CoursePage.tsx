import { useState, useEffect } from 'react';

interface CourseItem {
    id: number;
    name: string;
    description: string;
}

export function CoursePage() {
    const [allCourses, setAllCourses] = useState<CourseItem[]>([]);
    const [myCourses, setMyCourses] = useState<CourseItem[]>([]);

    // 获取全部课程
    const fetchAllCourses = async () => {
        const res = await fetch('/api/v1/courses');
        const json = await res.json();
        setAllCourses(json.items);
    };

    // 获取我的课程
    const fetchMyCourses = async () => {
        const res = await fetch('/api/v1/courses/mine');
        const json = await res.json();
        setMyCourses(json.items);
    };

    // 加入课程
    const handleJoinCourse = async (courseId: number) => {
        try {
            await fetch(`/api/v1/courses/${courseId}/join`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({})
            });
            // 加入成功刷新我的课程列表
            await fetchMyCourses();
        } catch (err) {
            console.error("加入课程失败：", err);
        }
    };

    useEffect(() => {
        fetchAllCourses();
        fetchMyCourses();
    }, []);

    return (
        <div style={{ padding: '16px' }}>
            <h2>我的课程</h2>
            {myCourses.length === 0 && <p>你还没有加入任何课程</p>}
            {myCourses.map((c) => (
                <div key={c.id} style={{ margin: '8px 0' }}>
                    <strong>{c.name}</strong>
                    <div>{c.description}</div>
                </div>
            ))}

            <h2>全部可加入课程</h2>
            {allCourses.map((c) => (
                <div key={c.id} style={{ margin: '8px 0' }}>
                    <span>{c.name}｜{c.description}</span>
                    <button onClick={() => handleJoinCourse(c.id)} style={{ marginLeft:'12px' }}>
                        加入课程
                    </button>
                </div>
            ))}
        </div>
    );
}
