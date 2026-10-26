import { useState } from "react";

// 模拟课程数据，Sprint0前端写死静态数据
const allCourseList = [
    {id: 1, title: "软件工程", desc: "Scrum敏捷开发、项目管理与软件项目实践"},
    {id: 2, title: "计算机网络", desc: "TCP/IP协议、网络分层与通信原理"},
    {id: 3, title: "操作系统", desc: "进程调度、内存管理与IO管理"},
]

export function CoursePage() {
    // 保存我已经加入的课程id
    const [myCourseIds, setMyCourseIds] = useState<number[]>([])

    // 加入课程点击事件
    const handleJoinCourse = (courseId: number) => {
        if(!myCourseIds.includes(courseId)){
            setMyCourseIds([...myCourseIds, courseId])
            alert("成功加入课程！");
        }
    }

    // 根据id拿到课程信息
    const myCourses = allCourseList.filter(c => myCourseIds.includes(c.id));

    return (
        <div style={{padding:"2rem", maxWidth:"1000px", margin:"0 auto"}}>
            <section style={{marginBottom:"3rem"}}>
                <h2 style={{textAlign:"center"}}>我的课程</h2>
                {
                    myCourses.length === 0
                        ? <p style={{textAlign:"center"}}>你还没有加入任何课程</p>
                        : (
                            <div style={{display:"grid", gridTemplateColumns:"1fr 1fr 1fr", gap:"16px", marginTop:"20px"}}>
                                {
                                    myCourses.map(item => (
                                        <div key={item.id} className="card" style={{border:"1px solid #ccc", padding:"16px", borderRadius:"8px"}}>
                                            <h3>{item.title}</h3>
                                            <p>{item.desc}</p>
                                        </div>
                                    ))
                                }
                            </div>
                        )
                }
            </section>

            <section>
                <h2 style={{textAlign:"center"}}>全部可加入课程</h2>
                <div style={{display:"grid", gridTemplateColumns:"1fr 1fr 1fr", gap:"16px", marginTop:"20px"}}>
                    {
                        allCourseList.map(course => (
                            <div key={course.id} style={{border:"1px solid #ccc", padding:"16px", borderRadius:"8px"}}>
                                <h3>{course.title}</h3>
                                <p>{course.desc}</p>
                                {
                                    myCourseIds.includes(course.id)
                                        ? <button disabled style={{marginTop:"10px", padding:"6px 12px"}}>已加入</button>
                                        : <button onClick={()=>handleJoinCourse(course.id)} style={{marginTop:"10px", padding:"6px 12px"}}>加入课程</button>
                                }
                            </div>
                        ))
                    }
                </div>
            </section>
        </div>
    )
}
